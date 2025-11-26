package com.example.xinqiao.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.xinqiao.R;
import com.example.xinqiao.model.Appointment;

import java.util.List;

/**
 * Adapter for counselor appointment list
 * Handles different appointment states (pending, confirmed, completed)
 */
public class CounselorAppointmentAdapter extends RecyclerView.Adapter<CounselorAppointmentAdapter.ViewHolder> {
    
    public interface OnAppointmentActionListener {
        void onAccept(Appointment appointment);
        void onReject(Appointment appointment);
        void onDetails(Appointment appointment);
        void onReview(Appointment appointment);
    }
    
    private Context context;
    private List<Appointment> appointments;
    private OnAppointmentActionListener listener;
    
    public CounselorAppointmentAdapter(Context context, List<Appointment> appointments) {
        this.context = context;
        this.appointments = appointments;
    }
    
    public void setOnAppointmentActionListener(OnAppointmentActionListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_counselor_appointment, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        holder.bind(appointment);
    }
    
    @Override
    public int getItemCount() {
        return appointments != null ? appointments.size() : 0;
    }
    
    public void updateData(List<Appointment> newAppointments) {
        this.appointments = newAppointments;
        notifyDataSetChanged();
    }
    
    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivUserAvatar;
        private TextView tvUserName;
        private TextView tvStatus;
        private TextView tvAppointmentTime;
        private TextView tvConsultationType;
        private LinearLayout llActionButtons;
        private Button btnAccept;
        private Button btnReject;
        private Button btnDetails;
        private Button btnReview;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvAppointmentTime = itemView.findViewById(R.id.tv_appointment_time);
            tvConsultationType = itemView.findViewById(R.id.tv_consultation_type);
            llActionButtons = itemView.findViewById(R.id.ll_action_buttons);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnDetails = itemView.findViewById(R.id.btn_details);
            btnReview = itemView.findViewById(R.id.btn_review);
        }
        
        public void bind(Appointment appointment) {
            // Set user info
            tvUserName.setText(appointment.getUserName());
            tvAppointmentTime.setText(appointment.getFormattedTime());
            tvConsultationType.setText(appointment.getConsultationTypeText());
            
            // Load avatar
            if (appointment.getUserAvatar() != null) {
                Glide.with(context)
                    .load(appointment.getUserAvatar())
                    .circleCrop()
                    .into(ivUserAvatar);
            } else {
                Glide.with(context)
                    .load(R.drawable.default_avatar)
                    .circleCrop()
                    .into(ivUserAvatar);
            }
            
            // Set status and corresponding actions
            setStatusAndActions(appointment);
            
            // Set up action listeners
            setupActionListeners(appointment);
        }
        
        private String getConsultationTypeText(String type) {
            switch (type) {
                case "video":
                    return "视频咨询";
                case "voice":
                    return "语音咨询";
                case "text":
                    return "文字咨询";
                default:
                    return "咨询";
            }
        }
        
        private void setStatusAndActions(Appointment appointment) {
            String status = appointment.getStatus();
            
            // Reset visibility
            btnAccept.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
            btnDetails.setVisibility(View.GONE);
            btnReview.setVisibility(View.GONE);
            
            switch (status) {
                case "pending":
                    tvStatus.setText("待处理");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    btnAccept.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    break;
                    
                case "confirmed":
                    tvStatus.setText("已确认");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
                    btnDetails.setVisibility(View.VISIBLE);
                    break;
                    
                case "completed":
                    tvStatus.setText("已完成");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
                    btnReview.setVisibility(View.VISIBLE);
                    break;
            }
        }
        
        private void setupActionListeners(final Appointment appointment) {
            btnAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onAccept(appointment);
                    }
                }
            });
            
            btnReject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onReject(appointment);
                    }
                }
            });
            
            btnDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onDetails(appointment);
                    }
                }
            });
            
            btnReview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onReview(appointment);
                    }
                }
            });
            
            // Item click listener for details
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null && !appointment.getStatus().equals("pending")) {
                        listener.onDetails(appointment);
                    }
                }
            });
        }
    }
}