package com.cliambrown.pilltime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.DialogFragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.text.format.DateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditDoseActivity extends SimpleMenuActivity {

    Button btn_editDose_save;
    EditText et_editDose_count;
    TextView tv_editDose_takenAtTime;
    TextView tv_editDose_takenAtDate;
    PillTimeApplication mApp;
    int medID, doseID;
    Calendar selectedDatetime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_dose);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        btn_editDose_save = findViewById(R.id.btn_editDose_save);
        et_editDose_count = findViewById(R.id.et_editDose_count);
        tv_editDose_takenAtTime = findViewById(R.id.tv_editDose_takenAtTime);
        tv_editDose_takenAtDate = findViewById(R.id.tv_editDose_takenAtDate);

        Intent intent = getIntent();
        medID = intent.getIntExtra("med_id", -1);
        doseID = intent.getIntExtra("dose_id", -1);
        mApp = (PillTimeApplication) this.getApplication();
        Med med = mApp.getMed(medID);
        Dose dose;

        if (med == null) {
            EditDoseActivity.this.finish();
        }

        selectedDatetime = Calendar.getInstance();
        if (doseID > -1) {
            dose = med.getDoseById(doseID);
            if (dose == null) {
                EditDoseActivity.this.finish();
            }
            setTitle(getString(R.string.edit) + " " + getString(R.string.dose));
            selectedDatetime.setTimeInMillis(dose.getTakenAt() * 1000L);
        } else {
            long now = System.currentTimeMillis() / 1000L;
            dose = new Dose(doseID, medID, med.getMaxDose(), now, EditDoseActivity.this);
            setTitle(getString(R.string.new_dose) + " — " + med.getName());
        }

        et_editDose_count.setText(Utils.getStrFromDbl(dose.getCount()));
        updateTimeField();
        updateDateField();

        tv_editDose_takenAtTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePickerDialog(view);
            }
        });

        tv_editDose_takenAtDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { showDatePickerDialog(view); }
        });

        btn_editDose_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                double count;

                try {
                    count = Double.parseDouble(et_editDose_count.getText().toString());
                } catch (Exception e) {
                    Toast.makeText(EditDoseActivity.this, "Error saving dose: invalid data", Toast.LENGTH_SHORT).show();
                    return;
                }

                long unixTime = selectedDatetime.getTimeInMillis() / 1000L;
                Dose dose = new Dose(doseID, medID, count, unixTime, EditDoseActivity.this);

                if (doseID > -1) {
                    boolean edited = mApp.setDose(med, dose);
                    if (!edited) return;
                } else {
                    boolean added = mApp.addDose(med, dose);
                    if (!added) return;
                }

                Toast.makeText(EditDoseActivity.this, "Dose saved", Toast.LENGTH_SHORT).show();

                EditDoseActivity.this.finish();
            }
        });
    }

    private void updateTimeField() {
        long unixTimeMs = selectedDatetime.getTimeInMillis();
        tv_editDose_takenAtTime.setText(DateUtils.formatDateTime(
                this,
                unixTimeMs,
                DateUtils.FORMAT_SHOW_TIME
        ));
    }

    private void updateDateField() {
        long unixTimeMs = selectedDatetime.getTimeInMillis();
        tv_editDose_takenAtDate.setText(DateUtils.formatDateTime(
                this,
                unixTimeMs,
                DateUtils.FORMAT_ABBREV_ALL |
                        DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_SHOW_WEEKDAY |
                        DateUtils.FORMAT_SHOW_YEAR
        ));
    }

    public static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int hour;
            int minute;
            EditDoseActivity activity = (EditDoseActivity) getActivity();
            if (activity != null) {
                hour = activity.selectedDatetime.get(Calendar.HOUR_OF_DAY);
                minute = activity.selectedDatetime.get(Calendar.MINUTE);
            } else {
                final Calendar c = Calendar.getInstance();
                hour = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);
            }

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            EditDoseActivity activity = (EditDoseActivity) getActivity();
            if (activity == null) return;
            activity.selectedDatetime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            activity.selectedDatetime.set(Calendar.MINUTE, minute);
            activity.updateTimeField();
        }
    }

    public void showTimePickerDialog(View v) {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int year;
            int month;
            int day;
            EditDoseActivity activity = (EditDoseActivity) getActivity();
            if (activity != null) {
                year = activity.selectedDatetime.get(Calendar.YEAR);
                month = activity.selectedDatetime.get(Calendar.MONTH);
                day = activity.selectedDatetime.get(Calendar.DAY_OF_MONTH);
            } else {
                final Calendar c = Calendar.getInstance();
                year = c.get(Calendar.YEAR);
                month = c.get(Calendar.MONTH);
                day = c.get(Calendar.DAY_OF_MONTH);
            }

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            EditDoseActivity activity = (EditDoseActivity) getActivity();
            if (activity == null) return;
            activity.selectedDatetime.set(Calendar.YEAR, year);
            activity.selectedDatetime.set(Calendar.MONTH, month);
            activity.selectedDatetime.set(Calendar.DAY_OF_MONTH, day);
            activity.updateDateField();
        }
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }
}