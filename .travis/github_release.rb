#!/usr/bin/env ruby

require 'optparse'
require 'octokit'

options = {}
OptionParser.new do |opt|
  opt.on('-s', '--secret SECRET', 'GitHub access token') { |o| options[:secret] = o }
  opt.on('-r', '--repo-slug REPO_SLUG', 'Repo slug. i.e.: apple/swift') { |o| options[:repo_slug] = o }
  opt.on('-c', '--changelog-file CHANGELOG_FILE', 'Changelog path') { |o| options[:changelog_file] = o }
  opt.on('-t', '--tag TAG', 'Tag name') { |o| options[:tag_name] = o }
end.parse!

raise OptionParser::MissingArgument if options[:secret].nil?
raise OptionParser::MissingArgument if options[:repo_slug].nil?
raise OptionParser::MissingArgument if options[:changelog_file].nil?
raise OptionParser::MissingArgument if options[:tag_name].nil?

client = Octokit::Client.new(:access_token => options[:secret])
user = client.user
user.login

unless client.scopes.include? 'public_repo' or client.scopes.include? 'repo'
  raise Error, "Insufficient permissions. Make sure your token contains the repo or public_repo scope."
end

puts "Logged in as #{user.name}"
puts "Deploying to repo: #{options[:repo_slug]}"

tag_matched = false
release_url = nil
releases = client.releases(options[:repo_slug])
body = File.open(options[:changelog_file], "rb").read

releases.each do |release|
  if release.tag_name == options[:tag_name]
    release_url = release.rels[:self].href
    tag_matched = true
  end
end

# if tag has been pushed directly to git, create a github release
if tag_matched == false
  client.create_release(options[:repo_slug], options[:tag_name], { :name => options[:tag_name], :body => body })
else
  client.update_release(release_url, { :name => options[:tag_name], :body => body })
end
