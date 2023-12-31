package com.example.todoapproomjetpackcompose

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapproomjetpackcompose.data.Todo
import com.example.todoapproomjetpackcompose.data.TodoStatus
import com.example.todoapproomjetpackcompose.ui.TodoViewModel
import com.example.todoapproomjetpackcompose.ui.ViewModelFactory
import com.example.todoapproomjetpackcompose.ui.theme.TodoAppRoomJetpackComposeTheme

// AppCompatActivityはフラグメントやアクションバーなどの機能が追加されたComponentActivityのサブクラス
// 上記の機能は必要ないので、ComponentActivityをインプリする
class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContent {
            TodoAppRoomJetpackComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // viewModelをviewへ繋ぎこむ
                    // Composable関数の中でviewmodelインスタンスを取得する場合はlifecycle-viewmodel-composeライブラリのviewModel()を使用する
                    val owner = LocalViewModelStoreOwner.current
                    owner?.let {
                        val viewModel = viewModel<TodoViewModel>(
                            it,
                            "TodoViewModel",
                            ViewModelFactory(LocalContext.current.applicationContext as Application)
                        )
                        MainContent(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: TodoViewModel) {
    LaunchedEffect(Unit) {
        viewModel.getTodoList()
    }
    
    val isShowDialog = remember { mutableStateOf(false) }

    Scaffold(floatingActionButton =  {
        FloatingActionButton(onClick = { isShowDialog.value = true }) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Button")
        }
    }) {
        if (isShowDialog.value) {
            EditDialog(isShowDialog = isShowDialog, viewModel = viewModel)
        }
        
        TodoList(viewModel = viewModel, isShowDialog)
    }
}

// Composable関数内で使用できるMutableStateはUI状態を保持するオブジェクト
// MutableState.valueが更新されると自動でComposable関数が再評価される(UIが更新される)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDialog(isShowDialog: MutableState<Boolean>, viewModel: TodoViewModel) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { isShowDialog.value = false },
        title = { Text(text = "create Todo") },
        text = {
            Column {
                Text(text = "title")
                OutlinedTextField(value = title, onValueChange = { title = it })
                Text(text = "description")
                OutlinedTextField(value = description, onValueChange = { description = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                isShowDialog.value = false
                if (viewModel.isUpdating()) {
                    viewModel.updateTodo(title, description)
                } else {
                    viewModel.addTodo(title, description)
                }
            }) {
                Text(text = "OK")
            }
        },
        dismissButton =  {
            Button(onClick = { isShowDialog.value = false }) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
fun TodoList(viewModel: TodoViewModel, isShowDialog: MutableState<Boolean>) {
    val observedTodos = viewModel.todos.observeAsState()
    observedTodos.value?.let { todos ->
        LazyColumn {
            items(todos) {
                TodoRow(it, viewModel, isShowDialog)
            }
        }
    }
}

@Composable
fun TodoRow(
    todo: Todo,
    viewModel: TodoViewModel,
    isShowDialog: MutableState<Boolean>
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .clickable {
                viewModel.setupTodo(todo)
                isShowDialog.value = true
            }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column() {
                Text(
                    text = todo.title,
                    fontStyle = FontStyle.Normal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp
                )
                Spacer(modifier = Modifier.weight(1F))
                Text(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    text = todo.description
                )
            }
            Spacer(modifier = Modifier.weight(1F))
            IconButton(onClick = { viewModel.delete(todo) }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
            }

            if (isShowDialog.value) {
                EditDialog(isShowDialog = isShowDialog, viewModel = viewModel)
            }
        }
    }
}