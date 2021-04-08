package com.codinginflow.mvvmtodo.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.codinginflow.mvvmtodo.R

import com.codinginflow.mvvmtodo.ui.tasks.TasksAdapter
import dagger.hilt.android.AndroidEntryPoint
import android.content.Context as Context1

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }




}