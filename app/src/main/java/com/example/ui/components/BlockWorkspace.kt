package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Block
import com.example.model.BlockType

// Theme Color Palette for Blocks
object BlockColors {
    val Comment = Color(0xFF64748B)       // Slate Grey
    val VarDecl = Color(0xFF8B5CF6)       // Purple
    val VarAssign = Color(0xFF3B82F6)     // Blue
    val Println = Color(0xFF06B6D4)       // Cyan/Teal
    val IfElse = Color(0xFFF59E0B)        // Amber/Orange
    val Loop = Color(0xFF10B981)          // Emerald Green
    
    val Background = Color(0xFF0F172A)    // Cyber Dark Slate
    val CardBg = Color(0xFF1E293B)        // Deep Gray Card
}

@Composable
fun BlockWorkspace(
    blocks: List<Block>,
    selectedBlockId: String?,
    highlightedBlockId: String?,
    onSelectBlock: (String?) -> Unit,
    onUpdateBlockFields: (String, String?, String?, String?) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onIndent: (String) -> Unit,
    onOutdent: (String) -> Unit,
    onAddBlock: (BlockType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddBlockDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(BlockColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Action Palette
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🧩 Visual AST Workspace",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Button(
                    onClick = { showAddBlockDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_block_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Block", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Block", fontSize = 13.sp)
                }
            }

            if (blocks.isEmpty()) {
                EmptyWorkspacePlaceholder(onAddFirstBlock = { showAddBlockDialog = true })
            } else {
                // Blocks Canvas List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(blocks, key = { it.id }) { rootBlock ->
                        RecursiveBlockItem(
                            block = rootBlock,
                            selectedBlockId = selectedBlockId,
                            highlightedBlockId = highlightedBlockId,
                            depth = 0,
                            onSelectBlock = onSelectBlock,
                            onUpdateBlockFields = onUpdateBlockFields,
                            onDeleteBlock = onDeleteBlock
                        )
                    }
                }
            }
        }

        // Structural Manipulator Panel at the very bottom if a block is selected
        AnimatedVisibility(
            visible = selectedBlockId != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            selectedBlockId?.let { selId ->
                StructuralControlsPanel(
                    blockId = selId,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                    onIndent = onIndent,
                    onOutdent = onOutdent,
                    onDelete = onDeleteBlock,
                    onDeselect = { onSelectBlock(null) }
                )
            }
        }

        // Add Block Category Drawer / Dialog
        if (showAddBlockDialog) {
            AddBlockDrawer(
                onDismiss = { showAddBlockDialog = false },
                onAdd = { type ->
                    onAddBlock(type)
                    showAddBlockDialog = false
                }
            )
        }
    }
}

@Composable
fun RecursiveBlockItem(
    block: Block,
    selectedBlockId: String?,
    highlightedBlockId: String?,
    depth: Int,
    onSelectBlock: (String?) -> Unit,
    onUpdateBlockFields: (String, String?, String?, String?) -> Unit,
    onDeleteBlock: (String) -> Unit
) {
    val isSelected = selectedBlockId == block.id
    val isHighlighted = highlightedBlockId == block.id

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
    ) {
        // Render current block card
        BlockCard(
            block = block,
            isSelected = isSelected,
            isHighlighted = isHighlighted,
            onSelect = { onSelectBlock(block.id) },
            onUpdateFields = { varName, varVal, comment ->
                onUpdateBlockFields(block.id, varName, varVal, comment)
            }
        )

        // Render children (body of If/Loop blocks)
        if (block.type == BlockType.IF_ELSE || block.type == BlockType.REPEAT || block.type == BlockType.WHILE_LOOP) {
            val blockColor = getBlockColor(block.type)
            
            Row(modifier = Modifier.fillMaxWidth()) {
                // Vertical puzzle piece connecting line on the left side of nested blocks
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 4.dp)
                        .width(4.dp)
                        .height(IntrinsicSize.Min)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(blockColor, blockColor.copy(alpha = 0.3f))
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                        .fillMaxHeight()
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (block.children.isEmpty()) {
                        // Tiny card for empty slots
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BlockColors.CardBg.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                        ) {
                            Text(
                                text = "💡 Drop code statements here",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        block.children.forEach { child ->
                            RecursiveBlockItem(
                                block = child,
                                selectedBlockId = selectedBlockId,
                                highlightedBlockId = highlightedBlockId,
                                depth = 0, // already offset by Row structure
                                onSelectBlock = onSelectBlock,
                                onUpdateBlockFields = onUpdateBlockFields,
                                onDeleteBlock = onDeleteBlock
                            )
                        }
                    }
                }
            }

            // If it's IF_ELSE, we also render the ELSE body block
            if (block.type == BlockType.IF_ELSE) {
                // Else Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .background(BlockColors.IfElse)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "else {",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = BlockColors.IfElse,
                        fontSize = 13.sp
                    )
                }

                // Else Body Children
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 4.dp)
                            .width(4.dp)
                            .height(IntrinsicSize.Min)
                            .background(BlockColors.IfElse.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            .fillMaxHeight()
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (block.elseChildren.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BlockColors.CardBg.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "💡 Else branch (optional) - Select an element inside else to add",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            block.elseChildren.forEach { child ->
                                RecursiveBlockItem(
                                    block = child,
                                    selectedBlockId = selectedBlockId,
                                    highlightedBlockId = highlightedBlockId,
                                    depth = 0,
                                    onSelectBlock = onSelectBlock,
                                    onUpdateBlockFields = onUpdateBlockFields,
                                    onDeleteBlock = onDeleteBlock
                                )
                            }
                        }
                    }
                }
            }

            // Close block brace
            Text(
                text = "}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = blockColor,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
fun BlockCard(
    block: Block,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onSelect: () -> Unit,
    onUpdateFields: (String?, String?, String?) -> Unit
) {
    val blockColor = getBlockColor(block.type)
    val highlightBorder = when {
        isHighlighted -> BorderStroke(3.dp, Color(0xFF10B981)) // Glow green on execution
        isSelected -> BorderStroke(2.dp, blockColor)            // Solid block border on select
        else -> BorderStroke(1.dp, blockColor.copy(alpha = 0.4f))
    }

    val elevationScale by animateFloatAsState(if (isHighlighted) 1.05f else 1.0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(elevationScale)
            .padding(vertical = 4.dp)
            .clickable { onSelect() }
            .testTag("block_card_${block.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BlockColors.CardBg else BlockColors.CardBg.copy(alpha = 0.9f)
        ),
        border = highlightBorder,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Block Header with Type Label & Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(blockColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getBlockIcon(block.type),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Keyword Text
                Text(
                    text = getBlockKeyword(block),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = blockColor,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                if (isHighlighted) {
                    Text(
                        text = "⚡ RUNNING",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Parameter Editor Controls inside Card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (block.type) {
                    BlockType.COMMENT -> {
                        OutlinedTextField(
                            value = block.commentText,
                            onValueChange = { onUpdateFields(null, null, it) },
                            placeholder = { Text("Write comment here...") },
                            textStyle = TextStyle(color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            modifier = Modifier.weight(1f).heightIn(max = 50.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = blockColor,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            singleLine = true
                        )
                    }
                    BlockType.VAR_DECL -> {
                        // Var Name
                        OutlinedTextField(
                            value = block.varName,
                            onValueChange = { onUpdateFields(it, null, null) },
                            label = { Text("var", fontSize = 10.sp) },
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            modifier = Modifier.weight(0.4f).heightIn(max = 55.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = blockColor,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            singleLine = true
                        )
                        Text("=", color = Color.Gray, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        // Var Value
                        OutlinedTextField(
                            value = block.varValue,
                            onValueChange = { onUpdateFields(null, it, null) },
                            label = { Text("Value", fontSize = 10.sp) },
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            modifier = Modifier.weight(0.6f).heightIn(max = 55.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = blockColor,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            singleLine = true
                        )
                    }
                    BlockType.VAR_ASSIGN -> {
                        // Var Name
                        OutlinedTextField(
                            value = block.varName,
                            onValueChange = { onUpdateFields(it, null, null) },
                            label = { Text("Assign to", fontSize = 10.sp) },
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            modifier = Modifier.weight(0.4f).heightIn(max = 55.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = blockColor,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            singleLine = true
                        )
                        Text("=", color = Color.Gray, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        // Var Value Expression
                        OutlinedTextField(
                            value = block.varValue,
                            onValueChange = { onUpdateFields(null, it, null) },
                            label = { Text("Expression", fontSize = 10.sp) },
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            modifier = Modifier.weight(0.6f).heightIn(max = 55.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = blockColor,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            singleLine = true
                        )
                    }
                    BlockType.PRINTLN -> {
                        OutlinedTextField(
                            value = block.varValue,
                            onValueChange = { onUpdateFields(null, it, null) },
                            label = { Text("Print Value", fontSize = 10.sp) },
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            modifier = Modifier.weight(1f).heightIn(max = 55.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = blockColor,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            singleLine = true
                        )
                    }
                    BlockType.IF_ELSE -> {
                        OutlinedTextField(
                            value = block.varValue,
                            onValueChange = { onUpdateFields(null, it, null) },
                            label = { Text("Condition (e.g. x < 5)", fontSize = 10.sp) },
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            modifier = Modifier.weight(1f).heightIn(max = 55.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = blockColor,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            singleLine = true
                        )
                    }
                    BlockType.REPEAT -> {
                        OutlinedTextField(
                            value = block.varValue,
                            onValueChange = { onUpdateFields(null, it, null) },
                            label = { Text("Repeat Times", fontSize = 10.sp) },
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            modifier = Modifier.weight(1f).heightIn(max = 55.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = blockColor,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            singleLine = true
                        )
                    }
                    BlockType.WHILE_LOOP -> {
                        OutlinedTextField(
                            value = block.varValue,
                            onValueChange = { onUpdateFields(null, it, null) },
                            label = { Text("While Condition", fontSize = 10.sp) },
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            modifier = Modifier.weight(1f).heightIn(max = 55.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = blockColor,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

// Visual Helpers
fun getBlockColor(type: BlockType): Color {
    return when (type) {
        BlockType.COMMENT -> BlockColors.Comment
        BlockType.VAR_DECL -> BlockColors.VarDecl
        BlockType.VAR_ASSIGN -> BlockColors.VarAssign
        BlockType.PRINTLN -> BlockColors.Println
        BlockType.IF_ELSE -> BlockColors.IfElse
        BlockType.REPEAT, BlockType.WHILE_LOOP -> BlockColors.Loop
    }
}

fun getBlockIcon(type: BlockType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        BlockType.COMMENT -> Icons.Default.Info
        BlockType.VAR_DECL -> Icons.Default.Add
        BlockType.VAR_ASSIGN -> Icons.Default.Create
        BlockType.PRINTLN -> Icons.Default.Terminal
        BlockType.IF_ELSE -> Icons.Default.AltRoute
        BlockType.REPEAT, BlockType.WHILE_LOOP -> Icons.Default.Refresh
    }
}

fun getBlockKeyword(block: Block): String {
    return when (block.type) {
        BlockType.COMMENT -> "comment"
        BlockType.VAR_DECL -> "var"
        BlockType.VAR_ASSIGN -> "assign"
        BlockType.PRINTLN -> "println"
        BlockType.IF_ELSE -> "if"
        BlockType.REPEAT -> "repeat"
        BlockType.WHILE_LOOP -> "while"
    }
}

@Composable
fun EmptyWorkspacePlaceholder(onAddFirstBlock: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Workspace is Empty",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add programming blocks like variables, conditions, loops, and print outputs to write logic.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddFirstBlock,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Insert My First Block")
            }
        }
    }
}

// Floating Panel for Block Operations
@Composable
fun StructuralControlsPanel(
    blockId: String,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onIndent: (String) -> Unit,
    onOutdent: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeselect: () -> Unit
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Edit Scope",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Move Up
                IconButton(
                    onClick = { onMoveUp(blockId) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.DarkGray)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                // Move Down
                IconButton(
                    onClick = { onMoveDown(blockId) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.DarkGray)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                // Indent
                IconButton(
                    onClick = { onIndent(blockId) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.DarkGray)
                ) {
                    Icon(Icons.Default.FormatIndentIncrease, contentDescription = "Nest Inside Predecessor", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                // Outdent
                IconButton(
                    onClick = { onOutdent(blockId) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.DarkGray)
                ) {
                    Icon(Icons.Default.FormatIndentDecrease, contentDescription = "Pull Out of Container", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                // Delete
                IconButton(
                    onClick = { onDelete(blockId) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
                // Done / Deselect
                IconButton(
                    onClick = onDeselect,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// Block Add Category Selection Box
@Composable
fun AddBlockDrawer(
    onDismiss: () -> Unit,
    onAdd: (BlockType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Block to Insert", color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF1E293B),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                // Comments Category
                AddBlockRow(
                    label = "Comment",
                    desc = "Standard text comment // to outline logic",
                    color = BlockColors.Comment,
                    icon = Icons.Default.Info,
                    onClick = { onAdd(BlockType.COMMENT) }
                )

                // Var Decl Category
                AddBlockRow(
                    label = "Variable Declaration",
                    desc = "Declare a new variable (var x = value)",
                    color = BlockColors.VarDecl,
                    icon = Icons.Default.Add,
                    onClick = { onAdd(BlockType.VAR_DECL) }
                )

                // Var Assign
                AddBlockRow(
                    label = "Variable Reassignment",
                    desc = "Modify value of an existing variable",
                    color = BlockColors.VarAssign,
                    icon = Icons.Default.Create,
                    onClick = { onAdd(BlockType.VAR_ASSIGN) }
                )

                // Print Category
                AddBlockRow(
                    label = "Print Statement",
                    desc = "Print messages or variable values to console",
                    color = BlockColors.Println,
                    icon = Icons.Default.Terminal,
                    onClick = { onAdd(BlockType.PRINTLN) }
                )

                // If/Else Category
                AddBlockRow(
                    label = "If/Else Condition",
                    desc = "Conditional decision branch statement",
                    color = BlockColors.IfElse,
                    icon = Icons.Default.AltRoute,
                    onClick = { onAdd(BlockType.IF_ELSE) }
                )

                // Loop category
                AddBlockRow(
                    label = "Repeat Loop",
                    desc = "Run nested code N times repeat(count)",
                    color = BlockColors.Loop,
                    icon = Icons.Default.Refresh,
                    onClick = { onAdd(BlockType.REPEAT) }
                )

                // While category
                AddBlockRow(
                    label = "While Loop",
                    desc = "Run nested code as long as condition is true",
                    color = BlockColors.Loop,
                    icon = Icons.Default.Refresh,
                    onClick = { onAdd(BlockType.WHILE_LOOP) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun AddBlockRow(
    label: String,
    desc: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text(desc, color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}
