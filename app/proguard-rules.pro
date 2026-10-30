# Keep line numbers and source file names honest in release builds without
# exposing the full source. The app has no reflection or serialization, so no
# keep rules are required.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
