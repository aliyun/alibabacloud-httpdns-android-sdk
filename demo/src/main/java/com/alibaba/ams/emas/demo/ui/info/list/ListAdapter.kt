package com.alibaba.ams.emas.demo.ui.info.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aliyun.ams.httpdns.demo.R
import com.aliyun.ams.httpdns.demo.databinding.InfoListItemBinding;

/**
 * @author allen.wy
 * @date 2023/6/5
 */
class ListAdapter(private val context: Context,
                  private val itemList: MutableList<ListItem>,
                  private val deleteListener: OnDeleteListener) :
    RecyclerView.Adapter<ListAdapter.ListViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = InfoListItemBinding.inflate(LayoutInflater.from(context))
        return ListViewHolder(context, binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        if (itemList.isEmpty()) {
            return
        }
        holder.setItemValue(itemList[position]) {
            when (itemList[holder.adapterPosition].type) {
                kListItemTypeHostWithFixedIP -> deleteListener.onHostWithFixedIPDeleted(holder.adapterPosition)
                kListItemTypeCacheTtl -> deleteListener.onTtlDeleted(itemList[holder.adapterPosition].content)
                kListItemPreResolve -> deleteListener.onPreResolveDeleted(holder.adapterPosition)
                else -> deleteListener.onIPRankingItemDeleted(holder.adapterPosition)
            }
            itemList.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    fun addItemData(item: ListItem) {
        itemList.add(item)
        notifyItemInserted(itemList.size - 1)
    }

    fun getPositionByContent(content: String): Int {
        for (index in itemList.indices) {
            if (content == itemList[index].content) {
                return index
            }
        }
        return -1
    }

    fun updateItemByPosition(content:String, intValue: Int, position: Int) {
        itemList[position].content = content
        itemList[position].intValue = intValue
        notifyItemChanged(position)
    }

    class ListViewHolder(private val context: Context, private val binding: InfoListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun setItemValue(listItem: ListItem, onDeleteListener: View.OnClickListener) {
            when (listItem.type) {
                kListItemTypeIPRanking -> {
                    binding.hostFixedIpContainer.visibility = View.GONE
                    binding.hostAndPortOrTtlContainer.visibility = View.VISIBLE
                    binding.hostValue.text = listItem.content
                    binding.portOrTtlValue.text = listItem.intValue.toString()
                    binding.portOrTtlIndicate.text = context.getString(R.string.port)
                }
                kListItemTypeCacheTtl -> {
                    binding.hostFixedIpContainer.visibility = View.GONE
                    binding.hostAndPortOrTtlContainer.visibility = View.VISIBLE
                    binding.hostValue.text = listItem.content
                    binding.portOrTtlValue.text = listItem.intValue.toString()
                    binding.portOrTtlIndicate.text = context.getString(R.string.ttl)
                }
                kListItemTypeHostWithFixedIP -> {
                    binding.hostFixedIpContainer.visibility = View.VISIBLE
                    binding.hostAndPortOrTtlContainer.visibility = View.GONE
                    binding.preHostOrWithFixedIp.text = listItem.content
                }
                kListItemPreResolve -> {
                    binding.hostFixedIpContainer.visibility = View.VISIBLE
                    binding.hostAndPortOrTtlContainer.visibility = View.GONE
                    binding.preHostOrWithFixedIp.text = listItem.content
                }
            }

            binding.slideDeleteMenu.setOnClickListener(onDeleteListener)
            binding.slideDeleteMenu2.setOnClickListener(onDeleteListener)
        }
    }

    interface OnDeleteListener {
        fun onHostWithFixedIPDeleted(position: Int)

        fun onIPRankingItemDeleted(position: Int)

        fun onTtlDeleted(host: String)

        fun onPreResolveDeleted(position: Int)
    }
}