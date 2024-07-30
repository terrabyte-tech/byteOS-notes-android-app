package com.terrabyte.byteosnotes

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.PrintWriter

class MainActivity : AppCompatActivity() {
  private lateinit var note_txtarea: EditText
  private lateinit var load_button: Button
  private lateinit var save_button: Button

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)

    note_txtarea = findViewById(R.id.note_txtarea)
    load_button = findViewById(R.id.load_button)
    save_button = findViewById(R.id.save_button)

    save_button.setOnClickListener{
      println("clicked save button")
      var fileName = "note.txt"
      var newFileContent = "note note note blah blah blah"

      File(fileName).writeText(newFileContent)
    }

    load_button.setOnClickListener{
      println("clicked load button")

//      dont have write permissions for android emulator...? need to figure out how to get around this to test?
      val noteFilePath = "output.txt" // Replace with the desired file path
      val noteFile = File(noteFilePath)
      val noteNewContent = "note note note blah blah blah"

      noteFile.writeText(noteNewContent)
      println("Text written to file successfully.")

//      File("somefile.txt").printWriter().use { out ->
//        history.forEach {
//          out.println("${it.key}, ${it.value}")
//        }
//      }
    }

    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }
  }
}