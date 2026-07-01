package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Project
import com.example.model.Block
import com.example.model.BlockType
import com.example.ui.components.AiTutorPanel
import com.example.ui.components.BlockWorkspace
import com.example.ui.components.ConsolePanel
import com.example.ui.components.KotlinCodeViewer
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LogicEditorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false, darkTheme = true) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LogicEditorViewModel = viewModel()
) {
    val blocks by viewModel.blocks.collectAsStateWithLifecycle()
    val selectedBlockId by viewModel.selectedBlockId.collectAsStateWithLifecycle()
    val highlightedBlockId by viewModel.highlightedBlockId.collectAsStateWithLifecycle()
    val isModified by viewModel.isModified.collectAsStateWithLifecycle()
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()

    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val consoleOutput by viewModel.consoleOutput.collectAsStateWithLifecycle()
    val variablesState by viewModel.variablesState.collectAsStateWithLifecycle()
    val stepDelayMs by viewModel.stepDelayMs.collectAsStateWithLifecycle()

    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    var showProjectsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kotlin Blocks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = currentProject?.name ?: "Untitled Program",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Projects Manager Button
                    IconButton(
                        onClick = { showProjectsDialog = true },
                        modifier = Modifier.testTag("projects_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Projects",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Run Play/Stop Button
                    if (isRunning) {
                        Button(
                            onClick = { viewModel.stopProgram() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp).testTag("stop_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("STOP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                activeTab = 2 // Switch to Console Tab to see live logs!
                                viewModel.startProgram()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp).testTag("play_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RUN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E293B),
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Tab 0: Blocks Workspace
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Extension, contentDescription = "Blocks") },
                    label = { Text("Workspace", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("tab_workspace")
                )

                // Tab 1: Code PREVIEW
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Code") },
                    label = { Text("Kotlin Code", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("tab_code")
                )

                // Tab 2: Console stdout
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal") },
                    label = { Text("Console", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("tab_console")
                )

                // Tab 3: Gemini Tutor
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Tutor") },
                    label = { Text("AI Tutor", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("tab_ai")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
        ) {
            when (activeTab) {
                0 -> {
                    BlockWorkspace(
                        blocks = blocks,
                        selectedBlockId = selectedBlockId,
                        highlightedBlockId = highlightedBlockId,
                        onSelectBlock = { id -> viewModel.selectBlock(id) },
                        onUpdateBlockFields = { id, name, value, comment ->
                            viewModel.updateBlockFields(id, name, value, comment)
                        },
                        onDeleteBlock = { id -> viewModel.deleteBlock(id) },
                        onMoveUp = { id -> viewModel.moveBlockUp(id) },
                        onMoveDown = { id -> viewModel.moveBlockDown(id) },
                        onIndent = { id -> viewModel.indentBlock(id) },
                        onOutdent = { id -> viewModel.outdentBlock(id) },
                        onAddBlock = { type -> viewModel.addBlock(type) }
                    )
                }
                1 -> {
                    KotlinCodeViewer(
                        code = viewModel.getGeneratedKotlinCode()
                    )
                }
                2 -> {
                    ConsolePanel(
                        consoleOutput = consoleOutput,
                        variables = variablesState,
                        stepDelayMs = stepDelayMs,
                        onSetStepDelay = { delay -> viewModel.setStepDelay(delay) },
                        onClearConsole = { viewModel.clearConsole() }
                    )
                }
                3 -> {
                    AiTutorPanel(
                        response = aiResponse,
                        isLoading = isAiLoading,
                        onAskGemini = { action ->
                            viewModel.askGemini(action)
                        }
                    )
                }
            }
        }
    }

    // Projects Manager Dialog
    if (showProjectsDialog) {
        ProjectsManagerDialog(
            currentProj = currentProject,
            projects = allProjects,
            onDismiss = { showProjectsDialog = false },
            onSave = { name -> viewModel.saveProject(name) },
            onLoad = { proj ->
                viewModel.loadProject(proj)
                showProjectsDialog = false
            },
            onDelete = { proj -> viewModel.deleteProject(proj) },
            onCreateNew = { name, template ->
                viewModel.createNewProject(name, template)
                showProjectsDialog = false
            }
        )
    }
}

@Composable
fun ProjectsManagerDialog(
    currentProj: Project?,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onLoad: (Project) -> Unit,
    onDelete: (Project) -> Unit,
    onCreateNew: (String, List<Block>) -> Unit
) {
    var projectNameInput by remember { mutableStateOf(currentProj?.name ?: "My Logic Program") }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workspace File Manager", color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF1E293B),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Save Current Program
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Save Current Program", fontWeight = FontWeight.Bold, color = Color.LightGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = projectNameInput,
                            onValueChange = { projectNameInput = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f).heightIn(max = 50.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (projectNameInput.trim().isNotEmpty()) {
                                    onSave(projectNameInput.trim())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("save_project_submit")
                        ) {
                            Text("Save", fontSize = 12.sp)
                        }
                    }
                }

                Divider(color = Color.DarkGray)

                // Section 2: Templates (Quick Create)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Starter Challenge Templates", fontWeight = FontWeight.Bold, color = Color.LightGray, fontSize = 12.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onCreateNew("Even/Odd Logic", Block.getEvenOddTemplate()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("Even/Odd", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onCreateNew("FizzBuzz Loop", Block.getFizzBuzzTemplate()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("FizzBuzz", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onCreateNew("Fibonacci Calc", Block.getFibonacciTemplate()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("Fibonacci", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { onCreateNew("Blank Program", emptyList()) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("Start Blank Program", fontSize = 11.sp)
                    }
                }

                Divider(color = Color.DarkGray)

                // Section 3: Saved Programs List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("My Saved Programs (${projects.size})", fontWeight = FontWeight.Bold, color = Color.LightGray, fontSize = 12.sp)
                    
                    if (projects.isEmpty()) {
                        Text("No saved programs yet.", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    } else {
                        projects.forEach { proj ->
                            val isActive = currentProj?.id == proj.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLoad(proj) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) Color(0xFF0F172A) else Color(0xFF131A26)
                                ),
                                border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = proj.name,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Modified: " + dateFormat.format(Date(proj.lastModified)),
                                            color = Color.Gray,
                                            fontSize = 9.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDelete(proj) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        }
    )
}
