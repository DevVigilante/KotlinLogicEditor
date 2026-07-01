package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Project
import com.example.data.ProjectRepository
import com.example.gemini.GeminiClient
import com.example.interpreter.KotlinInterpreter
import com.example.model.Block
import com.example.model.BlockType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class LogicEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    val allProjects: StateFlow<List<Project>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProjectRepository(database.projectDao())
        allProjects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Active project state
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _blocks = MutableStateFlow<List<Block>>(emptyList())
    val blocks: StateFlow<List<Block>> = _blocks.asStateFlow()

    private val _selectedBlockId = MutableStateFlow<String?>(null)
    val selectedBlockId: StateFlow<String?> = _selectedBlockId.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

    // Interpreter runner state
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _consoleOutput = MutableStateFlow("")
    val consoleOutput: StateFlow<String> = _consoleOutput.asStateFlow()

    private val _variablesState = MutableStateFlow<Map<String, Any>>(emptyMap())
    val variablesState: StateFlow<Map<String, Any>> = _variablesState.asStateFlow()

    private val _highlightedBlockId = MutableStateFlow<String?>(null)
    val highlightedBlockId: StateFlow<String?> = _highlightedBlockId.asStateFlow()

    private val _stepDelayMs = MutableStateFlow(300L) // Slow down for visual debugging by default
    val stepDelayMs: StateFlow<Long> = _stepDelayMs.asStateFlow()

    // AI State
    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private var interpreterJob: Job? = null

    // Initialize with a default clean project
    init {
        createNewProject("My First Logic")
    }

    fun setStepDelay(delay: Long) {
        _stepDelayMs.value = delay
    }

    fun selectBlock(id: String?) {
        _selectedBlockId.value = id
    }

    /**
     * Creates a new blank project in memory.
     */
    fun createNewProject(name: String, templateBlocks: List<Block> = emptyList()) {
        val defaultBlocks = templateBlocks.ifEmpty {
            listOf(
                Block(type = BlockType.COMMENT, commentText = "Welcome to Kotlin Visual Blocks!"),
                Block(type = BlockType.VAR_DECL, varName = "x", varValue = "5"),
                Block(type = BlockType.VAR_DECL, varName = "y", varValue = "10"),
                Block(type = BlockType.PRINTLN, varValue = "\"Sum of x and y is: \" + (x + y)")
            )
        }
        val proj = Project(
            id = 0, // transient, Room will generate
            name = name,
            blocksJson = Block.toJson(defaultBlocks),
            lastModified = System.currentTimeMillis()
        )
        _currentProject.value = proj
        _blocks.value = defaultBlocks
        _selectedBlockId.value = null
        _isModified.value = false
        clearConsole()
    }

    /**
     * Saves the current project state to the Room database.
     */
    fun saveProject(customName: String? = null) {
        val proj = _currentProject.value ?: return
        val finalName = customName ?: proj.name
        viewModelScope.launch {
            val updatedProj = proj.copy(
                name = finalName,
                blocksJson = Block.toJson(_blocks.value),
                lastModified = System.currentTimeMillis()
            )
            val newId = repository.insert(updatedProj)
            val savedProj = updatedProj.copy(id = if (proj.id == 0) newId.toInt() else proj.id)
            _currentProject.value = savedProj
            _isModified.value = false
        }
    }

    /**
     * Loads a project from the project list.
     */
    fun loadProject(project: Project) {
        stopProgram()
        _currentProject.value = project
        _blocks.value = Block.fromJson(project.blocksJson)
        _selectedBlockId.value = null
        _isModified.value = false
        clearConsole()
    }

    /**
     * Deletes a project from the database.
     */
    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.delete(project)
            if (_currentProject.value?.id == project.id) {
                createNewProject("New Program")
            }
        }
    }

    /**
     * Generates a concatenated Kotlin code string for the current blocks list.
     */
    fun getGeneratedKotlinCode(): String {
        val sb = StringBuilder()
        sb.append("fun main() {\n")
        _blocks.value.forEach {
            sb.append(it.toKotlinCode(1)).append("\n")
        }
        sb.append("}")
        return sb.toString()
    }

    // --- INTERPRETER ACTIONS ---

    fun startProgram() {
        if (_isRunning.value) return
        _isRunning.value = true
        _consoleOutput.value = ">>> Running Kotlin Block Program...\n"
        
        interpreterJob = viewModelScope.launch {
            val interpreter = KotlinInterpreter(
                onConsoleOutput = { text ->
                    _consoleOutput.value += text
                },
                onBlockHighlight = { id ->
                    _highlightedBlockId.value = id
                },
                onVariablesChanged = { vars ->
                    _variablesState.value = vars
                },
                isRunningCheck = { _isRunning.value }
            )
            interpreter.executeProgram(_blocks.value, _stepDelayMs.value)
            _isRunning.value = false
        }
    }

    fun stopProgram() {
        _isRunning.value = false
        interpreterJob?.cancel()
        interpreterJob = null
        _highlightedBlockId.value = null
    }

    fun clearConsole() {
        _consoleOutput.value = ""
        _variablesState.value = emptyMap()
    }

    // --- STRUCTURAL CODE TRANSFORM ACTIONS ---

    /**
     * Adds a new block of the specified type.
     * Inserts inside the selected container block, or appends at root.
     */
    fun addBlock(type: BlockType) {
        val newBlock = when (type) {
            BlockType.COMMENT -> Block(type = type, commentText = "New comment")
            BlockType.VAR_DECL -> Block(type = type, varName = "count", varValue = "0")
            BlockType.VAR_ASSIGN -> Block(type = type, varName = "count", varValue = "count + 1")
            BlockType.PRINTLN -> Block(type = type, varValue = "\"Step Count: \" + count")
            BlockType.IF_ELSE -> Block(type = type, varValue = "count < 5", children = emptyList(), elseChildren = emptyList())
            BlockType.REPEAT -> Block(type = type, varValue = "5", children = emptyList())
            BlockType.WHILE_LOOP -> Block(type = type, varValue = "count < 10", children = emptyList())
        }

        val selectedId = _selectedBlockId.value
        if (selectedId == null) {
            // Append to root list
            _blocks.value = _blocks.value + newBlock
        } else {
            // Insert inside container or as sibling
            _blocks.value = insertBlockInTree(_blocks.value, selectedId, newBlock)
        }
        _selectedBlockId.value = newBlock.id
        _isModified.value = true
    }

    fun deleteBlock(id: String) {
        _blocks.value = removeBlockFromTree(_blocks.value, id)
        if (_selectedBlockId.value == id) {
            _selectedBlockId.value = null
        }
        _isModified.value = true
    }

    fun moveBlockUp(id: String) {
        _blocks.value = moveBlockInTree(_blocks.value, id, moveUp = true)
        _isModified.value = true
    }

    fun moveBlockDown(id: String) {
        _blocks.value = moveBlockInTree(_blocks.value, id, moveUp = false)
        _isModified.value = true
    }

    fun indentBlock(id: String) {
        _blocks.value = indentBlockInTree(_blocks.value, id)
        _isModified.value = true
    }

    fun outdentBlock(id: String) {
        _blocks.value = outdentBlockInTree(_blocks.value, id)
        _isModified.value = true
    }

    fun updateBlockFields(id: String, varName: String? = null, varValue: String? = null, commentText: String? = null) {
        _blocks.value = updateBlockInTree(_blocks.value, id) { block ->
            block.copy(
                varName = varName ?: block.varName,
                varValue = varValue ?: block.varValue,
                commentText = commentText ?: block.commentText
            )
        }
        _isModified.value = true
    }

    // --- RECURSIVE AST MANIPULATION HELPERS ---

    private fun insertBlockInTree(list: List<Block>, selectedId: String, newBlock: Block): List<Block> {
        val result = mutableListOf<Block>()
        for (b in list) {
            result.add(b)
            if (b.id == selectedId) {
                // If it is a container, insert inside its children by default
                if (b.type == BlockType.IF_ELSE || b.type == BlockType.REPEAT || b.type == BlockType.WHILE_LOOP) {
                    result.removeAt(result.size - 1) // edit this block to have child
                    val updatedB = b.copy(children = listOf(newBlock) + b.children)
                    result.add(updatedB)
                } else {
                    // Sibling insert
                    result.add(newBlock)
                }
                continue
            }

            // Recurse children
            val childrenUpdated = insertBlockInTree(b.children, selectedId, newBlock)
            val elseChildrenUpdated = insertBlockInTree(b.elseChildren, selectedId, newBlock)
            if (childrenUpdated != b.children || elseChildrenUpdated != b.elseChildren) {
                result.removeAt(result.size - 1)
                result.add(b.copy(children = childrenUpdated, elseChildren = elseChildrenUpdated))
            }
        }
        return result
    }

    private fun removeBlockFromTree(list: List<Block>, id: String): List<Block> {
        val result = mutableListOf<Block>()
        for (b in list) {
            if (b.id == id) continue
            val childrenUpdated = removeBlockFromTree(b.children, id)
            val elseChildrenUpdated = removeBlockFromTree(b.elseChildren, id)
            result.add(b.copy(children = childrenUpdated, elseChildren = elseChildrenUpdated))
        }
        return result
    }

    private fun updateBlockInTree(list: List<Block>, id: String, transform: (Block) -> Block): List<Block> {
        return list.map { b ->
            if (b.id == id) {
                transform(b)
            } else {
                b.copy(
                    children = updateBlockInTree(b.children, id, transform),
                    elseChildren = updateBlockInTree(b.elseChildren, id, transform)
                )
            }
        }
    }

    private fun moveBlockInTree(list: List<Block>, id: String, moveUp: Boolean): List<Block> {
        // Try to find the target block inside this level's sibling list
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex in list.indices) {
                val mutable = list.toMutableList()
                val temp = mutable[index]
                mutable[index] = mutable[targetIndex]
                mutable[targetIndex] = temp
                return mutable
            }
            return list
        }

        // Recurse down children
        return list.map { b ->
            b.copy(
                children = moveBlockInTree(b.children, id, moveUp),
                elseChildren = moveBlockInTree(b.elseChildren, id, moveUp)
            )
        }
    }

    /**
     * Indent block: Nest it as a child inside its immediate predecessor if the predecessor is a container block.
     */
    private fun indentBlockInTree(list: List<Block>, id: String): List<Block> {
        val index = list.indexOfFirst { it.id == id }
        if (index > 0) {
            val predecessor = list[index - 1]
            if (predecessor.type == BlockType.IF_ELSE || predecessor.type == BlockType.REPEAT || predecessor.type == BlockType.WHILE_LOOP) {
                val targetBlock = list[index]
                val mutable = list.toMutableList()
                mutable.removeAt(index)
                
                // Nest inside predecessor's primary children list
                val updatedPredecessor = predecessor.copy(children = predecessor.children + targetBlock)
                mutable[index - 1] = updatedPredecessor
                return mutable
            }
        }

        // Recurse
        return list.map { b ->
            b.copy(
                children = indentBlockInTree(b.children, id),
                elseChildren = indentBlockInTree(b.elseChildren, id)
            )
        }
    }

    /**
     * Outdent block: Pull a child block out of its parent, placing it in the parent's sibling list right after the parent.
     */
    private fun outdentBlockInTree(list: List<Block>, id: String): List<Block> {
        // Find if target block is a child of any container in this level
        for (parent in list) {
            val childIndex = parent.children.indexOfFirst { it.id == id }
            if (childIndex != -1) {
                val childBlock = parent.children[childIndex]
                val updatedParent = parent.copy(children = parent.children.filter { it.id != id })
                
                val parentIndex = list.indexOf(parent)
                val mutable = list.toMutableList()
                mutable[parentIndex] = updatedParent
                mutable.add(parentIndex + 1, childBlock)
                return mutable
            }

            val elseChildIndex = parent.elseChildren.indexOfFirst { it.id == id }
            if (elseChildIndex != -1) {
                val childBlock = parent.elseChildren[elseChildIndex]
                val updatedParent = parent.copy(elseChildren = parent.elseChildren.filter { it.id != id })
                
                val parentIndex = list.indexOf(parent)
                val mutable = list.toMutableList()
                mutable[parentIndex] = updatedParent
                mutable.add(parentIndex + 1, childBlock)
                return mutable
            }
        }

        // Recurse
        return list.map { b ->
            b.copy(
                children = outdentBlockInTree(b.children, id),
                elseChildren = outdentBlockInTree(b.elseChildren, id)
            )
        }
    }

    // --- GEMINI AI ASSISTANT TRIGGERS ---

    fun askGemini(action: String) {
        _isAiLoading.value = true
        _aiResponse.value = "Consulting Gemini AI..."
        
        viewModelScope.launch {
            val code = getGeneratedKotlinCode()
            val prompt = when (action) {
                "explain" -> """
                    You are an expert Kotlin tutor for block-based visual programming. 
                    Explain this generated Kotlin code step-by-step for a beginner. 
                    Identify the variables, loops, and conditions in a friendly, conversational, and highly readable format.
                    Use bold text for keywords. Keep it brief (max 3 short paragraphs or clean bullet points).
                    
                    Here is the code:
                    ```kotlin
                    $code
                    ```
                """.trimIndent()
                "challenge" -> """
                    You are a friendly Kotlin coding coach. 
                    Look at this visual block program and suggest a fun coding challenge or extension that the student can try next.
                    For example, if they have a counter, suggest adding a step variable, or printing a specific message under a new condition.
                    Keep your suggestion highly encouraging, creative, and brief (max 4 bullet points).
                    
                    Here is the code:
                    ```kotlin
                    $code
                    ```
                """.trimIndent()
                "debug" -> """
                    You are a Kotlin logic checker. 
                    Inspect this visual block program's code for potential bugs, logical errors (like division by zero, infinite loops, variable re-use issues) or code smells. 
                    If there are no bugs, praise the student's clean coding logic! 
                    Explain clearly and kindly, and keep it very brief.
                    
                    Here is the code:
                    ```kotlin
                    $code
                    ```
                """.trimIndent()
                else -> "Explain this program: \n$code"
            }

            val response = GeminiClient.generateContent(prompt)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }
}
