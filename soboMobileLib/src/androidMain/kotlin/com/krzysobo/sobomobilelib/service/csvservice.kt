package com.krzysobo.sobomobilelib.service

//import com.opencsv.CSVReader
//import java.io.FileReader

class CsvService {
    // TODO TODO TODO
//    fun readCsvFromRaw(context: Context, rawResourceId: Int): File {
//        // Create a temporary file in the app's cache directory
//        val tempFile = File(context.cacheDir, "temp_csv_file.csv")
//
//        // Read the raw resource and write to the temporary file
//        context.resources.openRawResource(rawResourceId).use { input ->
//            FileOutputStream(tempFile).use { output ->
//                input.copyTo(output)
//            }
//        }
//        return tempFile
//    }
//    fun parseCsvFromRaw(context: Context, rawResourceId: Int) {
//        val csvFile = readCsvFromRaw(context, rawResourceId)
//        CSVReader(FileReader(csvFile)).use { reader ->
//            reader.forEach { row ->
//                println(row.joinToString(", "))
//            }
//        }
//    }

//    fun parseCsvFromUri(context: Context, uri: android.net.Uri) {
//        val csvFile = getFileFromUri(context, uri)
//        CSVReader(FileReader(csvFile)).use { reader ->
//            reader.forEach { row ->
//                println(row.joinToString(", "))
//            }
//        }
//    }

//    fun parseCsvFromInternalStorage(context: Context) {
//        val file = File(context.filesDir, "myfolder/data.csv")
//        if (file.exists()) {
//            CSVReader(FileReader(file)).use { reader ->
//                reader.forEach { row ->
//                    println(row.joinToString(", "))
//                }
//            }
//        }
//    }
}