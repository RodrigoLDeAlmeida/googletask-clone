package com.rodrigo.googletask_clone.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Classe de dados que representa a relação um-para-muitos entre uma Tarefa (pai)
 * e suas subtarefas (filhos).
 */
data class TaskWithSubtasks(
    // A tarefa pai
    @Embedded
    val task: Task,

    // A lista de subtarefas filhas
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId"
    )
    val subtasks: List<Task>
)
