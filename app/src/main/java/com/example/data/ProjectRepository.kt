package com.example.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Int): Project? {
        return projectDao.getProjectById(id)
    }

    suspend fun insert(project: Project): Long {
        return projectDao.insertProject(project)
    }

    suspend fun delete(project: Project) {
        projectDao.deleteProject(project)
    }

    suspend fun deleteById(id: Int) {
        projectDao.deleteProjectById(id)
    }
}
