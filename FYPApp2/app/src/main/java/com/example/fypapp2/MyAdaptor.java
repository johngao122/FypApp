package com.example.fypapp2;

import android.content.Context;
import android.content.Intent;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.view.View;


import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;



// Code for recycler view adaptor that shows all the iot mqtt message data
public class MyAdaptor extends RecyclerView.Adapter<MyAdaptor.MyViewHolder> {



    ArrayList<String> Name,Data;
    Context ct;


    public MyAdaptor(Context context, ArrayList<String> name, ArrayList<String> data){
        ct = context;
        Name = name;
        Data = data;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(ct);
        View view = inflater.inflate(R.layout.my_row,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.name.setText(Name.get(position));
        holder.data.setText(Data.get(position));
        holder.parentlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ct,settings.class);
                i.putExtra("Devicename",Name.get(position));
                ((Activity) ct).startActivityForResult(i,98);
            }
        });

    }

    @Override
    public int getItemCount() {
        return Name.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        TextView name,data;
        ImageView imageView;
        ConstraintLayout parentlayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.nameTextView);
            data = itemView.findViewById(R.id.Datatextview);
            imageView = itemView.findViewById(R.id.imageView);
            parentlayout = itemView.findViewById(R.id.parent_layout);
            imageView.setImageResource(R.drawable.microchip_ek3);
        }
    }
}
