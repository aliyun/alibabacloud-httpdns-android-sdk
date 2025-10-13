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
                kListItemTag -> deleteListener.onTagDeleted(holder.adapterPosition)
                kListItemTypeHostWithFixedIP -> deleteListener.onHostWithFixedIPDeleted(holder.adapterPosition)
                kListItemTypeBlackList -> deleteListener.onHostBlackListDeleted(holder.adapterPosition)
                kListItemTypeCacheTtl -> deleteListener.onTtlDeleted(itemList[holder.adapterPosition].content)
                kListItemPreResolve -> deleteListener.onPreResolveDeleted(itemList[holder.adapterPosition].content, itemList[holder.adapterPosition].intValue)
                kListItemBatchResolve -> deleteListener.onBatchResolveDeleted(itemList[holder.adapterPosition].content, itemList[holder.adapterPosition].intValue)
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
                kListItemTag -> {
                    binding.hostFixedIpContainer.visibility = View.VISIBLE
                    binding.hostAndPortOrTtlContainer.visibility = View.GONE
                    binding.preHostOrWithFixedIp.text = listItem.content
                }
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
                kListItemTypeBlackList -> {
                    binding.hostFixedIpContainer.visibility = View.VISIBLE
                    binding.hostAndPortOrTtlContainer.visibility = View.GONE
                    binding.preHostOrWithFixedIp.text = listItem.content
                }
                kListItemPreResolve -> {
                    binding.hostFixedIpContainer.visibility = View.GONE
                    binding.hostAndPortOrTtlContainer.visibility = View.VISIBLE
                    binding.hostValue.text = listItem.content
                    binding.portOrTtlValue.text = when (listItem.intValue) {
                        0 -> "IPv4"
                        1 -> "IPv6"
                        2 -> "IPv4&IPv6"
                        else -> "自动判断IP类型"
                    }
                    binding.portOrTtlIndicate.text = context.getString(R.string.ip_type)
                }
                kListItemBatchResolve -> {
                    binding.hostFixedIpContainer.visibility = View.GONE
                    binding.hostAndPortOrTtlContainer.visibility = View.VISIBLE
                    binding.hostValue.text = listItem.content
                    binding.portOrTtlValue.text = when (listItem.intValue) {
                        0 -> "IPv4"
                        1 -> "IPv6"
                        2 -> "IPv4&IPv6"
                        else -> "自动判断IP类型"
                    }
                    binding.portOrTtlIndicate.text = context.getString(R.string.ip_type)
                }
            }

            binding.slideDeleteMenu.setOnClickListener(onDeleteListener)
            binding.slideDeleteMenu2.setOnClickListener(onDeleteListener)
        }
    }

    interface OnDeleteListener {
        fun onTagDeleted(position: Int)

        fun onHostWithFixedIPDeleted(position: Int)

        fun onIPRankingItemDeleted(position: Int)

        fun onTtlDeleted(host: String)

        fun onPreResolveDeleted(host: String, intValue: Int)

        fun onHostBlackListDeleted(position: Int)

        fun onBatchResolveDeleted(host: String, intValue: Int)
    }
}