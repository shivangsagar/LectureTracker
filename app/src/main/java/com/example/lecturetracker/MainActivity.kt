package com.example.lecturetracker

import android.content.Context
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

import org.json.JSONArray
import org.json.JSONObject


// =========================================================
// SUBJECT DATA
// =========================================================

data class Subject(
    val id: Int,
    val name: String,
    val totalLectures: Int,
    val completedLectures: List<Boolean>
)


// =========================================================
// LOCAL STORAGE
// =========================================================

private const val PREFS_NAME = "lecture_tracker_data"
private const val SUBJECTS_KEY = "subjects"


// ---------------------------------------------------------
// SAVE SUBJECTS
// ---------------------------------------------------------

private fun saveSubjects(
    context: Context,
    subjects: List<Subject>
) {

    val jsonArray = JSONArray()

    subjects.forEach { subject ->

        val jsonObject = JSONObject()

        jsonObject.put(
            "id",
            subject.id
        )

        jsonObject.put(
            "name",
            subject.name
        )

        jsonObject.put(
            "totalLectures",
            subject.totalLectures
        )

        val completedArray = JSONArray()

        subject.completedLectures.forEach { completed ->

            completedArray.put(
                completed
            )
        }

        jsonObject.put(
            "completedLectures",
            completedArray
        )

        jsonArray.put(
            jsonObject
        )
    }

    context
        .getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        .edit()
        .putString(
            SUBJECTS_KEY,
            jsonArray.toString()
        )
        .apply()
}


// ---------------------------------------------------------
// LOAD SUBJECTS
// ---------------------------------------------------------

private fun loadSubjects(
    context: Context
): List<Subject> {

    val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    val savedData =
        preferences.getString(
            SUBJECTS_KEY,
            null
        ) ?: return emptyList()

    return try {

        val jsonArray =
            JSONArray(savedData)

        val result =
            mutableListOf<Subject>()

        for (i in 0 until jsonArray.length()) {

            val jsonObject =
                jsonArray.getJSONObject(i)

            val id =
                jsonObject.getInt(
                    "id"
                )

            val name =
                jsonObject.getString(
                    "name"
                )

            val totalLectures =
                jsonObject.getInt(
                    "totalLectures"
                )

            val completedArray =
                jsonObject.getJSONArray(
                    "completedLectures"
                )

            val completedLectures =
                List(
                    completedArray.length()
                ) { index ->

                    completedArray.getBoolean(
                        index
                    )
                }

            result.add(
                Subject(
                    id = id,
                    name = name,
                    totalLectures = totalLectures,
                    completedLectures =
                        completedLectures
                )
            )
        }

        result

    } catch (
        e: Exception
    ) {

        emptyList()
    }
}


// =========================================================
// POPUP POSITION PROVIDER
// =========================================================
//
// Positions Edit/Delete directly below the three-dot button.
// If there isn't enough space below, it moves above the button.
//

class SubjectMenuPositionProvider :
    PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {

        // Align popup's right edge
        // with the right edge of the three-dot button.

        val desiredX =
            anchorBounds.right -
                    popupContentSize.width


        // Normally place popup BELOW
        // the three-dot button.

        val desiredY =
            anchorBounds.bottom + 4


        // Prevent popup from going outside
        // left/right screen boundaries.

        val finalX =
            desiredX.coerceIn(
                0,
                (
                        windowSize.width -
                                popupContentSize.width
                        ).coerceAtLeast(0)
            )


        // If there is enough space below,
        // keep it below.
        // Otherwise place it above.

        val finalY =

            if (
                desiredY +
                popupContentSize.height <=
                windowSize.height
            ) {

                desiredY

            } else {

                (
                        anchorBounds.top -
                                popupContentSize.height -
                                4
                        ).coerceAtLeast(0)
            }


        return IntOffset(
            finalX,
            finalY
        )
    }
}


// =========================================================
// MAIN ACTIVITY
// =========================================================

class MainActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContent {

            LectureTrackerApp()
        }
    }
}


// =========================================================
// MAIN APP
// =========================================================

@Composable
fun LectureTrackerApp() {

    // -----------------------------------------------------
    // CONTEXT
    // -----------------------------------------------------

    val context =
        LocalContext.current


    // -----------------------------------------------------
    // SUBJECT LIST
    // -----------------------------------------------------

    val subjects =
        remember {

            mutableStateListOf<Subject>().apply {

                addAll(
                    loadSubjects(
                        context
                    )
                )
            }
        }


    // -----------------------------------------------------
    // CREATE DIALOG
    // -----------------------------------------------------

    var showAddDialog by remember {

        mutableStateOf(false)
    }


    // -----------------------------------------------------
    // EDIT DIALOG
    // -----------------------------------------------------

    var editingSubject by remember {

        mutableStateOf<Subject?>(null)
    }


    // -----------------------------------------------------
    // NEXT ID
    // -----------------------------------------------------

    var nextId by remember {

        mutableStateOf(
            (
                    subjects.maxOfOrNull {
                        it.id
                    } ?: 0
                    ) + 1
        )
    }


    // =====================================================
    // DARK THEME
    // =====================================================

    MaterialTheme(

        colorScheme =
            androidx.compose.material3
                .darkColorScheme(

                    background =
                        Color.Black,

                    surface =
                        Color.Black,

                    surfaceContainer =
                        Color.Black,

                    primary =
                        Color.White,

                    onPrimary =
                        Color.Black,

                    onBackground =
                        Color.White,

                    onSurface =
                        Color.White
                )
    ) {


        // =================================================
        // SCAFFOLD
        // =================================================

        Scaffold(

            containerColor =
                Color.Black

        ) { paddingValues ->


            // =================================================
            // MAIN CONTENT
            // =================================================

            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black
                        )
                        .padding(
                            paddingValues
                        )
                        .padding(
                            horizontal = 20.dp
                        )
            ) {


                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )


                // =================================================
                // APP TITLE + CREATE BUTTON
                // =================================================

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {


                    // ---------------------------------------------
                    // TITLE
                    // ---------------------------------------------

                    Text(

                        text =
                            "Lecture Tracker",

                        color =
                            Color.White,

                        fontSize =
                            28.sp,

                        fontWeight =
                            FontWeight.Bold,

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )


                    // ---------------------------------------------
                    // CREATE (+) BUTTON
                    // ---------------------------------------------

                    Box(

                        modifier =
                            Modifier
                                .size(
                                    52.dp
                                )
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    Color.White
                                )
                                .clickable {

                                    showAddDialog =
                                        true
                                },

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Add,

                            contentDescription =
                                "Create Subject",

                            tint =
                                Color.Black,

                            modifier =
                                Modifier.size(
                                    28.dp
                                )
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            28.dp
                        )
                )


                // =================================================
                // SUBJECTS
                // =================================================

                if (
                    subjects.isEmpty()
                ) {

                    Box(

                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(

                            text =
                                "No subjects added yet",

                            color =
                                Color.White,

                            fontSize =
                                17.sp
                        )
                    }

                } else {


                    // =================================================
                    // SUBJECT LIST
                    // =================================================

                    LazyColumn(

                        modifier =
                            Modifier.fillMaxSize(),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                28.dp
                            )
                    ) {

                        items(

                            items =
                                subjects,

                            key = {
                                it.id
                            }

                        ) { subject ->


                            SubjectSection(

                                subject =
                                    subject,


                                // -----------------------------------------
                                // TOGGLE LECTURE
                                // -----------------------------------------

                                onToggleLecture =
                                    { index ->

                                        val subjectIndex =
                                            subjects.indexOfFirst {

                                                it.id ==
                                                        subject.id
                                            }


                                        if (
                                            subjectIndex != -1
                                        ) {

                                            val updatedLectures =
                                                subject
                                                    .completedLectures
                                                    .toMutableList()


                                            if (
                                                index in
                                                updatedLectures.indices
                                            ) {

                                                updatedLectures[
                                                    index
                                                ] =
                                                    !updatedLectures[
                                                        index
                                                    ]


                                                subjects[
                                                    subjectIndex
                                                ] =
                                                    subject.copy(

                                                        completedLectures =
                                                            updatedLectures
                                                                .toList()
                                                    )


                                                // SAVE IMMEDIATELY
                                                saveSubjects(
                                                    context,
                                                    subjects.toList()
                                                )
                                            }
                                        }
                                    },


                                // -----------------------------------------
                                // EDIT
                                // -----------------------------------------

                                onEdit = {

                                    editingSubject =
                                        subject
                                },


                                // -----------------------------------------
                                // DELETE
                                // -----------------------------------------

                                onDelete = {

                                    subjects.removeAll {

                                        it.id ==
                                                subject.id
                                    }


                                    // SAVE IMMEDIATELY
                                    saveSubjects(
                                        context,
                                        subjects.toList()
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }


        // =================================================
        // CREATE DIALOG
        // =================================================

        if (
            showAddDialog
        ) {

            AddSubjectDialog(

                onDismiss = {

                    showAddDialog =
                        false
                },


                onAdd =
                    { name, totalLectures ->

                        subjects.add(

                            Subject(

                                id =
                                    nextId++,

                                name =
                                    name,

                                totalLectures =
                                    totalLectures,

                                completedLectures =
                                    List(
                                        totalLectures
                                    ) {
                                        false
                                    }
                            )
                        )


                        // SAVE IMMEDIATELY
                        saveSubjects(
                            context,
                            subjects.toList()
                        )


                        showAddDialog =
                            false
                    }
            )
        }


        // =================================================
        // EDIT DIALOG
        // =================================================

        editingSubject?.let { subject ->

            EditSubjectDialog(

                subject =
                    subject,


                onDismiss = {

                    editingSubject =
                        null
                },


                onSave =
                    { newName, newTotal ->

                        val subjectIndex =
                            subjects.indexOfFirst {

                                it.id ==
                                        subject.id
                            }


                        if (
                            subjectIndex != -1
                        ) {


                            // -------------------------------------
                            // PRESERVE OLD LECTURE STATUS
                            // -------------------------------------

                            val updatedLectures =
                                List(
                                    newTotal
                                ) { index ->

                                    if (
                                        index <
                                        subject
                                            .completedLectures
                                            .size
                                    ) {

                                        subject
                                            .completedLectures[
                                            index
                                        ]

                                    } else {

                                        false
                                    }
                                }


                            subjects[
                                subjectIndex
                            ] =
                                subject.copy(

                                    name =
                                        newName,

                                    totalLectures =
                                        newTotal,

                                    completedLectures =
                                        updatedLectures
                                )


                            // SAVE IMMEDIATELY
                            saveSubjects(
                                context,
                                subjects.toList()
                            )
                        }


                        editingSubject =
                            null
                    }
            )
        }
    }
}


// =========================================================
// SUBJECT SECTION
// =========================================================

@Composable
fun SubjectSection(

    subject: Subject,

    onToggleLecture: (Int) -> Unit,

    onEdit: () -> Unit,

    onDelete: () -> Unit
) {


    // -----------------------------------------------------
    // COMPLETED
    // -----------------------------------------------------

    val completed =
        subject.completedLectures.count {
            it
        }


    // -----------------------------------------------------
    // REMAINING
    // -----------------------------------------------------

    val remaining =
        subject.totalLectures -
                completed


    // -----------------------------------------------------
    // PERCENTAGE
    // -----------------------------------------------------

    val percentage =

        if (
            subject.totalLectures == 0
        ) {

            0

        } else {

            (
                    (
                            completed.toFloat() /
                                    subject.totalLectures
                            ) * 100
                    ).toInt()
        }


    // -----------------------------------------------------
    // MENU STATE
    // -----------------------------------------------------

    var menuExpanded by remember {

        mutableStateOf(false)
    }


    Column(

        modifier =
            Modifier.fillMaxWidth()
    ) {


        // =================================================
        // SUBJECT NAME + THREE DOT
        // =================================================

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            // ---------------------------------------------
            // SUBJECT INFORMATION
            // ---------------------------------------------

            Column(

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        subject.name,

                    color =
                        Color.White,

                    fontSize =
                        21.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(

                    text =
                        "${subject.totalLectures} Lectures",

                    color =
                        Color.White,

                    fontSize =
                        14.sp
                )
            }


            // =================================================
            // THREE DOT
            // =================================================

            Box {

                IconButton(

                    onClick = {

                        menuExpanded =
                            true
                    }
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.MoreVert,

                        contentDescription =
                            "Subject Options",

                        tint =
                            Color.White,

                        modifier =
                            Modifier.size(
                                28.dp
                            )
                    )
                }


                // =================================================
                // CUSTOM MENU
                // =================================================

                if (
                    menuExpanded
                ) {

                    Popup(

                        popupPositionProvider =
                            SubjectMenuPositionProvider(),

                        onDismissRequest = {

                            menuExpanded =
                                false
                        },

                        properties =
                            PopupProperties(
                                focusable = true
                            )
                    ) {

                        Column(

                            modifier =
                                Modifier
                                    .width(
                                        105.dp
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            7.dp
                                        )
                                    )
                        ) {


                            // =====================================
                            // EDIT
                            // =====================================

                            Box(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(
                                            43.dp
                                        )
                                        .background(
                                            Color.White
                                        )
                                        .clickable {

                                            menuExpanded =
                                                false

                                            onEdit()
                                        }
                                        .padding(
                                            horizontal =
                                                14.dp
                                        ),

                                contentAlignment =
                                    Alignment.CenterStart
                            ) {

                                Text(

                                    text =
                                        "Edit",

                                    color =
                                        Color.Black,

                                    fontSize =
                                        16.sp,

                                    fontWeight =
                                        FontWeight.Medium
                                )
                            }


                            // =====================================
                            // DELETE
                            // =====================================

                            Box(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(
                                            43.dp
                                        )
                                        .background(
                                            Color(0xFFD32F2F)
                                        )
                                        .clickable {

                                            menuExpanded =
                                                false

                                            onDelete()
                                        }
                                        .padding(
                                            horizontal =
                                                14.dp
                                        ),

                                contentAlignment =
                                    Alignment.CenterStart
                            ) {

                                Text(

                                    text =
                                        "Delete",

                                    color =
                                        Color.White,

                                    fontSize =
                                        16.sp,

                                    fontWeight =
                                        FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }


        // =================================================
        // SPACE BEFORE BOXES
        // =================================================

        Spacer(
            modifier =
                Modifier.height(
                    14.dp
                )
        )


        // =================================================
        // EXACTLY 10 BOXES PER ROW
        // =================================================

        BoxWithConstraints(

            modifier =
                Modifier.fillMaxWidth()
        ) {


            val spacing =
                4.dp


            // -------------------------------------------------
            // 10 boxes + 9 spaces = available width
            // -------------------------------------------------

            val calculatedSize =
                (
                        maxWidth -
                                (
                                        spacing * 9
                                        )
                        ) / 10


            // -------------------------------------------------
            // Maximum box size = 34dp
            // -------------------------------------------------

            val boxSize =
                calculatedSize.coerceAtMost(
                    34.dp
                )


            Column(

                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {


                // ---------------------------------------------
                // CREATE ROWS OF 10
                // ---------------------------------------------

                subject.completedLectures
                    .chunked(10)
                    .forEachIndexed {

                            rowIndex,
                            row ->

                        Row(

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    spacing
                                )
                        ) {

                            row.forEachIndexed {

                                    columnIndex,
                                    isCompleted ->

                                val actualIndex =
                                    rowIndex * 10 +
                                            columnIndex


                                LectureBox(

                                    isCompleted =
                                        isCompleted,

                                    boxSize =
                                        boxSize,

                                    onClick = {

                                        onToggleLecture(
                                            actualIndex
                                        )
                                    }
                                )
                            }
                        }
                    }
            }
        }


        // =================================================
        // SPACE AFTER BOXES
        // =================================================

        Spacer(
            modifier =
                Modifier.height(
                    26.dp
                )
        )


        // =================================================
        // REMAINING + DONUT
        // =================================================

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        75.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            // ---------------------------------------------
            // REMAINING LECTURES
            // ---------------------------------------------

            Box(

                modifier =
                    Modifier.weight(
                        1f
                    ),

                contentAlignment =
                    Alignment.CenterStart
            ) {

                Text(

                    text =
                        "$remaining Remaining lectures",

                    color =
                        Color.White,

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Medium
                )
            }


            // ---------------------------------------------
            // SMALL DONUT
            // ---------------------------------------------

            DonutChart(

                percentage =
                    percentage,

                modifier =
                    Modifier.size(
                        70.dp
                    )
            )
        }
    }
}


// =========================================================
// LECTURE BOX
// =========================================================

@Composable
fun LectureBox(

    isCompleted: Boolean,

    boxSize: Dp,

    onClick: () -> Unit
) {

    Box(

        modifier =
            Modifier
                .size(
                    boxSize
                )
                .clip(
                    RoundedCornerShape(
                        5.dp
                    )
                )

                // Completed = white
                // Empty = black

                .background(

                    if (
                        isCompleted
                    ) {

                        Color.White

                    } else {

                        Color.Black
                    }
                )

                // White border

                .border(

                    width =
                        2.dp,

                    color =
                        Color.White,

                    shape =
                        RoundedCornerShape(
                            5.dp
                        )
                )

                .clickable {

                    onClick()
                }
    )
}


// =========================================================
// DONUT CHART
// =========================================================

@Composable
fun DonutChart(

    percentage: Int,

    modifier: Modifier =
        Modifier
) {

    Box(

        modifier =
            modifier,

        contentAlignment =
            Alignment.Center
    ) {


        // =================================================
        // DONUT
        // =================================================

        Canvas(

            modifier =
                Modifier.fillMaxSize()
        ) {

            val strokeWidth =
                8.dp.toPx()


            // ---------------------------------------------
            // BACKGROUND RING
            // ---------------------------------------------

            drawArc(

                color =
                    Color.DarkGray,

                startAngle =
                    -90f,

                sweepAngle =
                    360f,

                useCenter =
                    false,

                style =
                    Stroke(

                        width =
                            strokeWidth,

                        cap =
                            StrokeCap.Round
                    )
            )


            // ---------------------------------------------
            // COMPLETED RING
            // ---------------------------------------------

            if (
                percentage > 0
            ) {

                drawArc(

                    color =
                        Color.White,

                    startAngle =
                        -90f,

                    sweepAngle =
                        percentage * 3.6f,

                    useCenter =
                        false,

                    style =
                        Stroke(

                            width =
                                strokeWidth,

                            cap =
                                StrokeCap.Round
                        )
                )
            }
        }


        // =================================================
        // PERCENTAGE
        // =================================================

        Text(

            text =
                "$percentage%",

            color =
                Color.White,

            fontSize =
                14.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}


// =========================================================
// CREATE SUBJECT DIALOG
// =========================================================

@Composable
fun AddSubjectDialog(

    onDismiss: () -> Unit,

    onAdd: (String, Int) -> Unit
) {


    var subjectName by remember {

        mutableStateOf("")
    }


    var lectureCount by remember {

        mutableStateOf("")
    }


    AlertDialog(

        onDismissRequest =
            onDismiss,

        containerColor =
            Color(0xFF111111),


        title = {

            Text(

                text =
                    "Create Subject",

                color =
                    Color.White,

                fontWeight =
                    FontWeight.Bold
            )
        },


        text = {

            Column {


                // -----------------------------------------
                // SUBJECT NAME
                // -----------------------------------------

                OutlinedTextField(

                    value =
                        subjectName,

                    onValueChange = {

                        subjectName =
                            it
                    },

                    label = {

                        Text(

                            text =
                                "Subject Name",

                            color =
                                Color.White
                        )
                    },

                    singleLine =
                        true
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                // -----------------------------------------
                // TOTAL LECTURES
                // -----------------------------------------

                OutlinedTextField(

                    value =
                        lectureCount,

                    onValueChange = {

                        if (
                            it.all {
                                    character ->
                                character.isDigit()
                            }
                        ) {

                            lectureCount =
                                it
                        }
                    },

                    label = {

                        Text(

                            text =
                                "Total Lectures",

                            color =
                                Color.White
                        )
                    },

                    keyboardOptions =
                        KeyboardOptions(

                            keyboardType =
                                KeyboardType.Number
                        ),

                    singleLine =
                        true
                )
            }
        },


        // =================================================
        // CREATE
        // =================================================

        confirmButton = {

            Button(

                onClick = {

                    val total =
                        lectureCount
                            .toIntOrNull()


                    if (

                        subjectName.isNotBlank() &&

                        total != null &&

                        total > 0

                    ) {

                        onAdd(

                            subjectName.trim(),

                            total
                        )
                    }
                },

                colors =
                    ButtonDefaults
                        .buttonColors(

                            containerColor =
                                Color.White,

                            contentColor =
                                Color.Black
                        )
            ) {

                Text(

                    text =
                        "Create",

                    color =
                        Color.Black
                )
            }
        },


        // =================================================
        // CANCEL
        // =================================================

        dismissButton = {

            TextButton(

                onClick =
                    onDismiss
            ) {

                Text(

                    text =
                        "Cancel",

                    color =
                        Color.White
                )
            }
        },


        properties =
            DialogProperties(

                dismissOnBackPress =
                    true,

                dismissOnClickOutside =
                    true
            )
    )
}


// =========================================================
// EDIT SUBJECT DIALOG
// =========================================================

@Composable
fun EditSubjectDialog(

    subject: Subject,

    onDismiss: () -> Unit,

    onSave: (String, Int) -> Unit
) {


    var subjectName by remember(

        subject.id

    ) {

        mutableStateOf(
            subject.name
        )
    }


    var lectureCount by remember(

        subject.id

    ) {

        mutableStateOf(
            subject.totalLectures
                .toString()
        )
    }


    AlertDialog(

        onDismissRequest =
            onDismiss,

        containerColor =
            Color(0xFF111111),


        title = {

            Text(

                text =
                    "Edit Subject",

                color =
                    Color.White,

                fontWeight =
                    FontWeight.Bold
            )
        },


        text = {

            Column {


                // -----------------------------------------
                // SUBJECT NAME
                // -----------------------------------------

                OutlinedTextField(

                    value =
                        subjectName,

                    onValueChange = {

                        subjectName =
                            it
                    },

                    label = {

                        Text(

                            text =
                                "Subject Name",

                            color =
                                Color.White
                        )
                    },

                    singleLine =
                        true
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                // -----------------------------------------
                // NUMBER OF LECTURES
                // -----------------------------------------

                OutlinedTextField(

                    value =
                        lectureCount,

                    onValueChange = {

                        if (
                            it.all {
                                    character ->
                                character.isDigit()
                            }
                        ) {

                            lectureCount =
                                it
                        }
                    },

                    label = {

                        Text(

                            text =
                                "Number of Lectures",

                            color =
                                Color.White
                        )
                    },

                    keyboardOptions =
                        KeyboardOptions(

                            keyboardType =
                                KeyboardType.Number
                        ),

                    singleLine =
                        true
                )
            }
        },


        // =================================================
        // OK
        // =================================================

        confirmButton = {

            Button(

                onClick = {

                    val total =
                        lectureCount
                            .toIntOrNull()


                    if (

                        subjectName.isNotBlank() &&

                        total != null &&

                        total > 0

                    ) {

                        onSave(

                            subjectName.trim(),

                            total
                        )
                    }
                },

                colors =
                    ButtonDefaults
                        .buttonColors(

                            containerColor =
                                Color.White,

                            contentColor =
                                Color.Black
                        )
            ) {

                Text(

                    text =
                        "OK",

                    color =
                        Color.Black
                )
            }
        },


        // =================================================
        // CANCEL
        // =================================================

        dismissButton = {

            TextButton(

                onClick =
                    onDismiss
            ) {

                Text(

                    text =
                        "Cancel",

                    color =
                        Color.White
                )
            }
        },


        properties =
            DialogProperties(

                dismissOnBackPress =
                    true,

                dismissOnClickOutside =
                    true
            )
    )
}