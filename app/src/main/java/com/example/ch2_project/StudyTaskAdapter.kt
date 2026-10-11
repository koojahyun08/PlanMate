package com.example.ch2_project

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 학습 일정 화면 전용 심플 어댑터
class StudyTaskAdapter(
    private var taskList: MutableList<TaskItem>,
    private val onCheckChanged: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<StudyTaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.taskCheckBox)
        val contentText: TextView = itemView.findViewById(R.id.taskContentText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task_row, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        // 🚀 핵심 해결법: 체크박스 상태를 코드로 변경하기 전에 리스너를 잠시 꺼둡니다. (무한 루프 방지)
        holder.checkBox.setOnCheckedChangeListener(null)

        holder.contentText.text = task.content
        holder.checkBox.isChecked = task.isCompleted

        // 체크 상태에 따른 취소선 및 색상 디자인
        if (task.isCompleted) {
            holder.contentText.paintFlags = holder.contentText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.contentText.setTextColor(0xFFBBBBBB.toInt())
        } else {
            holder.contentText.paintFlags = holder.contentText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.contentText.setTextColor(0xFF333333.toInt())
        }

        // 디자인과 상태 세팅이 모두 끝난 후 리스너를 다시 달아줍니다.
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            task.isCompleted = isChecked
            onCheckChanged(task.taskId, isChecked)

            // position 대신 holder.adapterPosition을 사용하는 것이 더 안전합니다.
            notifyItemChanged(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateData(newTasks: List<TaskItem>) {
        taskList.clear()
        taskList.addAll(newTasks)
        notifyDataSetChanged()
    }
}