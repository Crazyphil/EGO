package tk.crazysoft.ego.data;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StandbyImporter extends Importer {
    private Pattern quarterPattern, stringPattern, numberPattern, timeWildCardPattern, hMinSeperator, anyPattern;
    private Matcher quarterMatcher, timeWildCardMatcher;
    private int quarter, year, timeCount;
    private boolean monthsPassed, timeWildCardsPassed;
    private HashMap<String, int[]> timeList;
    private ArrayList<ContentValues> valueList;
    private ArrayList<String> timeWildCardList;
    private Calendar date;

    public StandbyImporter(Context context) {
        super(context);

        quarterPattern = Pattern.compile("^\\w+\\s+(\\d)/(\\d{4})$");
        stringPattern = Pattern.compile("^\\w+$");
        numberPattern = Pattern.compile("^\\d{1,2}$");
        timeWildCardPattern = Pattern.compile("(?:\\s*(\\w+)(?:\\s*\\w*\\s*)*(\\d{2}(?::\\d{2})?)-(\\d{1,2}:\\d{2})\\s*)");
        hMinSeperator = Pattern.compile(":");
        anyPattern = Pattern.compile(".+");

        monthsPassed = false;
        timeWildCardsPassed = false;
        timeList = new HashMap<String, int[]>();
        valueList = new ArrayList<ContentValues>();
        timeWildCardList = new ArrayList<String>();

        date = GregorianCalendar.getInstance();
        date.setLenient(false);
    }

    @Override
    public int process(String[] line) {
        if (quarter == 0 || year == 0) {
            try {
                quarterMatcher = quarterPattern.matcher(line[0]);
                if (quarterMatcher.find()) {
                    try {
                        quarter = Integer.parseInt(quarterMatcher.group(1));
                        year = Integer.parseInt(quarterMatcher.group(2));
                    } catch (NumberFormatException e) {
                        return PROCESS_ERROR;
                    }

                    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                    if (year > currentYear + 1) {
                        return PROCESS_ERROR;
                    }

                    if (quarter < 1 || quarter > 4) {
                        return PROCESS_ERROR;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return PROCESS_ERROR;
            }
        }

        else if (!monthsPassed) {
            try {
                if (stringPattern.matcher(line[2]).find() && !(anyPattern.matcher(line[line.length-1]).find())) {
                    monthsPassed = true;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return PROCESS_ERROR;
            }
        }

        else if (timeWildCardList.isEmpty()) {
            try {
                if (stringPattern.matcher(line[2]).find() && stringPattern.matcher(line[line.length-1]).find()) {
                    initializeTimeWildCardList(line);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return PROCESS_ERROR;
            }
        }

        else if (numberPattern.matcher(line[0]).find() && stringPattern.matcher(line[1]).find()) {
            try {
                return processStandbyDataLine(line);
            } catch (ArrayIndexOutOfBoundsException e) {
                return PROCESS_ERROR;
            }

        }

        else if (!timeWildCardsPassed) {
            try {
                timeWildCardMatcher = timeWildCardPattern.matcher(line[0]);
            } catch (ArrayIndexOutOfBoundsException e) {
                return PROCESS_ERROR;
            }

            if (timeWildCardMatcher.find()) {
                int index = 0;
                //for (int i = 1; i == timeCount; i+=3) {
                while (timeWildCardMatcher.find(index)) {
                    index = timeWildCardMatcher.end();
                    try {
                        initializeTimeMap();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        return PROCESS_ERROR;
                    }
                }
            }
        }

        return PROCESS_IGNORED;
    }

    @Override
    public void postProcess() {
        if (timeList.isEmpty()) {
            return;
        }

        // Replace WildCards
        String wildCard;
        int from, to;
        int[] timeArray;

        getDatabase().beginTransaction();

        for (ContentValues v : valueList) {
            wildCard = v.get(EGOContract.DoctorStandby.COLUMN_NAME_TIME_FROM).toString();

            timeArray = timeList.get(wildCard);
            if (timeArray == null) {
                return;
            }

            from = timeArray[0];
            to = timeArray[1];

            v.put(EGOContract.DoctorStandby.COLUMN_NAME_TIME_FROM, from);

            if (from > to) {
                ContentValues nextValue = new ContentValues();
                date.setTimeInMillis(v.getAsLong(EGOContract.DoctorStandby.COLUMN_NAME_DATE)*1000);
                date.add(Calendar.DATE, 1);

                try {
                    // Provoke an IllegalArgumentException if the date is not valid
                    date.get(Calendar.DATE);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                nextValue.put(EGOContract.DoctorStandby.COLUMN_NAME_DATE, date.getTimeInMillis() / 1000);


                String name = v.getAsString(EGOContract.DoctorStandby.COLUMN_NAME_DOCTOR_NAME);
                nextValue.put(EGOContract.DoctorStandby.COLUMN_NAME_DOCTOR_NAME, name);

                nextValue.put(EGOContract.DoctorStandby.COLUMN_NAME_TIME_FROM, 0);
                nextValue.put(EGOContract.DoctorStandby.COLUMN_NAME_TIME_TO, to);

                long nextid = getDatabase().insert(EGOContract.DoctorStandby.TABLE_NAME, null, nextValue);
                if (nextid == -1) {
                    getDatabase().endTransaction();
                    return;
                }
                v.put(EGOContract.DoctorStandby.COLUMN_NAME_NEXT_ID, nextid);

                v.put(EGOContract.DoctorStandby.COLUMN_NAME_TIME_TO, 1440); // 24*60 = 1440
            } else {
                v.put(EGOContract.DoctorStandby.COLUMN_NAME_TIME_TO, to);
            }

            if (getDatabase().insert(EGOContract.DoctorStandby.TABLE_NAME, null, v) == -1) {
                getDatabase().endTransaction();
                return;
            }
        }
        getDatabase().setTransactionSuccessful();
        getDatabase().endTransaction();
    }

    private void initializeTimeMap() throws ArrayIndexOutOfBoundsException {
        String wildCard, fromTimeS, toTimeS;
        int fromTimeI, toTimeI;

        wildCard = timeWildCardMatcher.group(1);
        fromTimeS = timeWildCardMatcher.group(2);
        toTimeS = timeWildCardMatcher.group(3);

        String[] timeS = hMinSeperator.split(fromTimeS);
        if (timeS.length == 2) {
            fromTimeI = Integer.parseInt(timeS[0])*60 + Integer.parseInt(timeS[1]);
        } else {
            fromTimeI = Integer.parseInt(timeS[0])*60;
        }

        timeS = hMinSeperator.split(toTimeS);
        toTimeI = Integer.parseInt(timeS[0])*60 + Integer.parseInt(timeS[1]);

        int[] timeI = {fromTimeI, toTimeI};
        timeList.put(wildCard, timeI);
    }

    private ContentValues createValues(String[] line, int monthIndex, int timeSlot) {
        int lineIndex;
        ContentValues values = new ContentValues();
        values.put(EGOContract.DoctorStandby.COLUMN_NAME_DATE, date.getTimeInMillis() /1000);
        lineIndex = 1+monthIndex+timeSlot+(monthIndex-1)*timeCount;
        if (!anyPattern.matcher(line[lineIndex]).find()) {
            return null;
        }
        values.put(EGOContract.DoctorStandby.COLUMN_NAME_DOCTOR_NAME, line[lineIndex]);
        values.put(EGOContract.DoctorStandby.COLUMN_NAME_TIME_FROM, timeWildCardList.get(timeSlot));
        values.put(EGOContract.DoctorStandby.COLUMN_NAME_TIME_TO, timeWildCardList.get(timeSlot));
        return values;
    }

    private void initializeTimeWildCardList(String[] line) throws ArrayIndexOutOfBoundsException {
        // Length of the line minus day, weekday (3 times) and holiday column
        timeCount = (line.length - 5)/3;

        String lineText;
        for (int i = 2; i < timeCount+2; i++) {
            lineText = line[i];
            if (anyPattern.matcher(lineText).find()) {
                timeWildCardList.add(lineText);
            }
        }
    }

    private int processStandbyDataLine(String[] line) throws ArrayIndexOutOfBoundsException {
        int month, day;

        for (int monthIndex = 1; monthIndex <=3; monthIndex++) {
            month = (quarter - 1) * 3 + monthIndex;

            try {
                day = Integer.parseInt(line[0]);
                date.set(year, month - 1, day, 0, 0, 0);

                // Provoke an IllegalArgumentException if the date is not valid
                date.get(Calendar.DATE);
            } catch (IllegalArgumentException e) {
                continue;
            }

            for (int timeSlot = 0; timeSlot < timeCount; timeSlot++) {

                ContentValues values = createValues(line, monthIndex, timeSlot);
                if (values != null) {
                    valueList.add(values);
                }
            }
        }
        return PROCESS_SUCCESS;
    }
}
