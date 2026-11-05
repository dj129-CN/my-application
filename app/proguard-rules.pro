# Room generates implementations at compile time. Keep entity metadata used by
# reflection-free generated code and the schema identity.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# WorkManager instantiates workers by class name.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
