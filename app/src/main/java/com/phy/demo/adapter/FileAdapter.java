package com.phy.demo.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.phy.demo.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 文件适配器
 * @author llw
 */
public class FileAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public FileAdapter(int layoutResId, @Nullable List<String> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder helper, String name) {
        helper.setText(R.id.tv_file_name,name);
    }
}
