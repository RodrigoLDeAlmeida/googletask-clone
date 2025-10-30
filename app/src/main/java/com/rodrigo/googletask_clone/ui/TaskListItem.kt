package com.rodrigo.googletask_clone.ui

import com.rodrigo.googletask_clone.data.Task

/**
 * Representa os diferentes tipos de itens que podem aparecer na lista de tarefas principal.
 * Usar uma classe selada nos permite lidar com os diferentes tipos de forma segura e explícita.
 */
sealed class TaskListItem {
    /**
     * Um item que representa uma tarefa (seja pai, filho ou concluída).
     */
    data class TaskItem(val task: Task, val subtaskCount: Int = 0) : TaskListItem() {
        override val id = task.id.toLong()
    }

    /**
     * Um item que representa o cabeçalho da seção "Concluídas".
     */
    data class HeaderItem(val completedCount: Int) : TaskListItem() {
        override val id = Long.MIN_VALUE // ID único e estável para o cabeçalho
    }

    abstract val id: Long
}
