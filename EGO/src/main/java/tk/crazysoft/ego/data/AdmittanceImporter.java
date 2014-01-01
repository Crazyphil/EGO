package tk.crazysoft.ego.data;

import android.content.ContentValues;
import android.content.Context;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import tk.crazysoft.ego.preferences.Preferences;

public class AdmittanceImporter extends Importer {
    private int year;
    private Pattern hospitalSeparator;
    private int takeoverTime;

    public AdmittanceImporter(Context context) {
        super(context);

        hospitalSeparator = Pattern.compile("/");
        Preferences preferences = new Preferences(getContext());
        takeoverTime = preferences.getHospitalsDoctorsTakeover().toInt();
    }

    @Override
    public int process(String[] line) {
        if (line.length < 13) {
            return PROCESS_ERROR;
        }

        if (year == 0) {
            try {
                year = Integer.parseInt(csvTrim(line[0]));
            } catch (NumberFormatException e) {
                return PROCESS_ERROR;
            }
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if (year < currentYear - 10 || year > currentYear + 10) {
                return PROCESS_ERROR;
            }
            deleteYear();
            return PROCESS_IGNORED;
        }

        try {
            int day = Integer.parseInt(csvTrim(line[0]));
            Calendar date = GregorianCalendar.getInstance();
            date.setLenient(false);
            for (int i = 1; i < 13; i++) {
                String hospital = csvTrim(line[i]);
                if (!hospital.isEmpty()) {
                    try {
                        date.set(year, i - 1, day, 0, 0, 0);

                        // Provoke an IllegalArgumentException if the date is not valid
                        date.get(Calendar.DATE);
                    } catch (IllegalArgumentException e) {
                        return PROCESS_ERROR;
                    }

                    if (!insertAdmittance(date, hospital)) {
                        return PROCESS_ERROR;
                    }
                }
            }
        } catch (NumberFormatException e) {
            return PROCESS_ERROR;
        }
        return PROCESS_SUCCESS;
    }

    private boolean deleteYear() {
        long yearBegin = new GregorianCalendar(year, Calendar.JANUARY, 1, 0, 0, 0).getTimeInMillis() / 1000;
        long yearEnd = new GregorianCalendar(year, Calendar.DECEMBER, 31, 23, 59, 59).getTimeInMillis() / 1000;
        return getDatabase().delete(EGOContract.HospitalAdmission.TABLE_NAME, EGOContract.HospitalAdmission.COLUMN_NAME_DATE + " BETWEEN ? AND ?", new String[] { String.valueOf(yearBegin), String.valueOf(yearEnd) }) > 0;
    }

    private boolean insertAdmittance(Calendar date, String hospital) {
        if (hospitalSeparator.matcher(hospital).find()) {
            boolean result = true;
            String[] hospitals = hospitalSeparator.split(hospital);
            for (String entry : hospitals) {
                result = result && insertAdmittance(date, csvTrim(entry));
            }
            return result;
        }

        ContentValues values = new ContentValues();
        values.put(EGOContract.HospitalAdmission.COLUMN_NAME_DATE, date.getTimeInMillis() / 1000);
        values.put(EGOContract.HospitalAdmission.COLUMN_NAME_TAKEOVER_TIME, takeoverTime);
        values.put(EGOContract.HospitalAdmission.COLUMN_NAME_HOSPITAL_NAME, hospital);
        return getDatabase().insert(EGOContract.HospitalAdmission.TABLE_NAME, null, values) > -1;
    }
}
