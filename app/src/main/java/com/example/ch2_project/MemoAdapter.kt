package com.example.ch2_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 1. 서버에서 받아올 메모 데이터 그릇 (설계도)
data class Memo(
    val id: Int,
    val title: String,
    val content: String,
    val created_at: String
)

// 2. 리사이클러뷰와 데이터를 연결해주는 어댑터
class MemoAdapter(
    private var memoList: MutableList<Memo>,
    private val onEditClick: (Memo) -> Unit,
    private val onDeleteClick: (Memo) -> Unit
) : RecyclerView.Adapter<MemoAdapter.MemoViewHolder>() {

    // item_memo.xml 안에 있는 화면 요소(글씨, 버튼 등)들을 찾아주는 뷰홀더
    inner class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.memoTitle)
        val dateText: TextView = itemView.findViewById(R.id.memoDate)
        val contentText: TextView = itemView.findViewById(R.id.memoContent)
        val editButton: TextView = itemView.findViewById(R.id.editButton)
        val deleteButton: TextView = itemView.findViewById(R.id.deleteButton)
    }

    // 화면에 보여줄 카드(item_memo.xml)를 찍어내는 함수
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_memo, parent, false)
        return MemoViewHolder(view)
    }

    // 찍어낸 카드에 실제 메모 데이터를 예쁘게 채워넣는 함수
    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        val memo = memoList[position]

        holder.titleText.text = memo.title
        holder.contentText.text = memo.content

        // 서버에서 오는 날짜(예: 2024-03-20T12:00:00)에서 "T" 앞부분(날짜)만 잘라서 표시
        holder.dateText.text = memo.created_at.substringBefore("T")

        // 수정 버튼을 누르면 MemoActivity로 신호를 보냄
        holder.editButton.setOnClickListener {
            onEditClick(memo)
        }

        // 삭제 버튼을 누르면 MemoActivity로 신호를 보냄
        holder.deleteButton.setOnClickListener {
            onDeleteClick(memo)
        }
    }

    // 전체 메모가 몇 개인지 알려주는 함수
    override fun getItemCount(): Int {
        return memoList.size
    }

    // 서버에서 최신 메모 목록을 당겨왔을 때 화면을 새로고침 하는 함수
    fun updateData(newMemos: List<Memo>) {
        memoList.clear()
        memoList.addAll(newMemos)
        notifyDataSetChanged() // 화면 갱신 얍!
    }
}