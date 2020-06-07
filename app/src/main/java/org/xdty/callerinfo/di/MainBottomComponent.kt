package org.xdty.callerinfo.di

import dagger.Component
import org.xdty.callerinfo.di.modules.AppModule
import org.xdty.callerinfo.di.modules.MainBottomModule
import org.xdty.callerinfo.fragment.MainBottomSheetFragment
import javax.inject.Singleton

@Singleton
@Component(modules = [MainBottomModule::class, AppModule::class])
interface MainBottomComponent {
    fun inject(mainBottomSheetFragment: MainBottomSheetFragment)
}