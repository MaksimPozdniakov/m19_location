package project.moms.attractions.data

import android.app.Application
import androidx.room.Room
import com.yandex.mapkit.MapKitFactory

class App : Application() {

    lateinit var db: GalleryDataBase
    override fun onCreate() {
        super.onCreate()

        db = Room.databaseBuilder(
            applicationContext,
            GalleryDataBase::class.java,
            "db"
        ).build()

        MapKitFactory.setApiKey("7dcdf03c-1b01-40bb-bbb6-049ea4d25642")
        MapKitFactory.initialize(this)
    }
}