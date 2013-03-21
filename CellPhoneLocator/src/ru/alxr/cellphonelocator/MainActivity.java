package ru.alxr.cellphonelocator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	MetroData md = new MetroData();
	final String fileForLists = "metroDataLists.dat";
	final String justTack = "justTack.txt";
    TelephonyManager tm, tm2;
	List <NeighboringCellInfo> nci;
	GsmCellLocation gsmcl;
	CellLocation cl;
	TextView tv, // информация о gsm 
	         tv2; // информация о станции
	static String networkOperatorName;
	static int lac=3, cid=3, lacLast=4, cidLast=4;
	static String currentGlobalLine = "Не выбрана";
	static String currentGlobalStation = "Не выбрана";
	static int currentGlobalStationId = 224;
	
	static String _getNetworkOperator;
	static int _lastSignalStrength = 0;
	
	static int maxId=101;
	static int countOfVibro = 0;
	static String messageFromDeep="";
	static String textSomethingChanges = "";
	static String startOfApp = "Приложение запущено ";
	static boolean q1=true;
	SharedPreferences prefs;
	static int checkStationCount = 0;
	
	
//	PhoneStateListener  listener;
    
    
    
	


	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (TextView)findViewById(R.id.textView1);
		tv2 = (TextView)findViewById(R.id.textView2);
        prefs = this.getSharedPreferences(
	    	      "ru.alxr.CellPhonelocator", Context.MODE_PRIVATE);
        currentGlobalStationId = prefs.getInt("ru.alxr.CellPhonelocator", 225);
                
        if (q1) 
		{
			q1 = false;
			Date date = new Date();
	        SimpleDateFormat sdf = new SimpleDateFormat();
	        sdf.applyPattern("yy/MM/dd/ kk:mm:ss");
			startOfApp = startOfApp + sdf.format(date); 
			}
        findStationById(currentGlobalStationId);
        toPublicStationInfoOnScreen();
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tm2 = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		
		

        tm.listen(new PhoneStateListener(){ 
        	@Override 
            public void onSignalStrengthChanged (int signalStrength) {
             _lastSignalStrength = signalStrength;
            }
        	public void onCellLocationChanged(CellLocation location) 
        	   {whereAmI();Date date = new Date();SimpleDateFormat sdf = new SimpleDateFormat();
        	   sdf.applyPattern("yy/MM/dd/ kk:mm:ss.SSS");
        	   if ((cid!=cidLast)||(lac!=lacLast)){
        	   textSomethingChanges  = "Что-то поменялось "+ sdf.format(date)+" (" +
        	                                     ((Integer)(++countOfVibro)).toString()+")";
        	   }
        	   else {
        		   textSomethingChanges  = "Что-то поменялось "+ sdf.format(date)+" (" +
                       ((Integer)(++countOfVibro)).toString()+")"+ " ложное :(";
        		   }
        	   toPublicGsmOnScreen("");
        	   Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        		// Vibrate for 300 milliseconds
        		v.vibrate(300);
        		createFileInHiddenArea(justTack);
        		copyFileFromHiddenToSharedArea(justTack);}
        	      
        },  PhoneStateListener.LISTEN_CELL_LOCATION);
        
        tm2.listen(new PhoneStateListener(){ 
        	@Override 
            public void onSignalStrengthChanged (int signalStrength) {
             _lastSignalStrength = signalStrength;
            }
        	},  PhoneStateListener.LISTEN_SIGNAL_STRENGTH);

        whereAmI();
        toPublicGsmOnScreen("");
		
        
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		for (int l = 0; l< md.simpleBase.size(); l++)
		{
			try {
				String str1="";
				str1 = md.simpleBase.get(l).getLineName();
				SubMenu subMenu = menu.addSubMenu(str1);
				int size = md.simpleBase.get(l).stations.size();
				for (int j=0 ; j < size; j++)
				{
					int stId = md.simpleBase.get(l).stations.get(j).id;
					String stNm = md.simpleBase.get(l).stations.get(j).station;
					subMenu.add(Menu.NONE, stId, Menu.NONE, stNm);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				tv.append(" exception onCreateOptionsMenu ");
			}
		}
		return super.onCreateOptionsMenu(menu);
	}
	
	
	
	public void toPublicStationInfoOnScreen(){
		tv2.setText("");
		tv2.append("Текущая линия:   "+ currentGlobalLine+ "\n");
        tv2.append("Текущая станция: "+ currentGlobalStation+
        		" (id"+ ((Integer)(currentGlobalStationId)).toString()+ ")\n" + startOfApp);		
	}
	
	public void toPublicGsmOnScreen(String message){
		String messageToScreen = 
				"Оператор: "+ networkOperatorName+"\n" +
						"cid = "+ ((Integer)(cid)).toString() + " (было "+  ((Integer)(cidLast)).toString()  +")\n"+
						"lac = "+ ((Integer)(lac)).toString() + " (было "+  ((Integer)(lacLast)).toString()  +")\n"+ 
						"MCC+MNC: " + _getNetworkOperator +"\n"+
						"FORCE: " + ((Integer)(_lastSignalStrength)).toString() +"\n"+
						message + textSomethingChanges + messageFromDeep;
		tv.setText(messageToScreen);
	}

	
	
	
	
	public void findStationById (int _id){
		for (SimpleLine line : md.simpleBase){
			for (SimpleStation st : line.stations)
			{
				if (st.id==_id) {
					currentGlobalLine = line.lineName;
					currentGlobalStation = st.station;
					currentGlobalStationId = st.id;
					break;
				}
			}
		}
	}

	
public void whereAmI (){
	try {

		nci =  tm.getNeighboringCellInfo();
		messageFromDeep ="";
		if (nci == null) {
			messageFromDeep= " getNeighboringCellInfo is NULL " + "\n";
			
		} else {
			if (nci.size()>0){
				messageFromDeep ="nci.size = " + ((Integer)(nci.size())).toString() + "\n";
				createFileInHiddenArea(fileForLists);
				copyFileFromHiddenToSharedArea(fileForLists);
			}
		}
		networkOperatorName = tm.getNetworkOperatorName ();
		gsmcl= (GsmCellLocation)(tm.getCellLocation());
		cidLast = cid;
		lacLast = lac;
		cid = gsmcl.getCid();
		lac = gsmcl.getLac();
		_getNetworkOperator = tm.getNetworkOperator();
		} catch (Exception e) 
		    {
			toPublicGsmOnScreen(" Информация о сотовой связи недоступна!" + "\n");
			}
	
}
	
	public void onClick (View v){
		switch (v.getId()){
		case R.id.button3 : // кнопка "влево", т.е. id--
		{
			if (currentGlobalStationId>101)	currentGlobalStationId--;
			else currentGlobalStationId = maxId;
			findStationById(currentGlobalStationId);
			toPublicStationInfoOnScreen();
			whereAmI();
			toPublicGsmOnScreen("");
			

			
			break;
		}
		
		case R.id.button4 : // кнопка "вправо", т.е. id++
		{
			if (currentGlobalStationId==maxId || currentGlobalStationId<101) currentGlobalStationId=101;
			else currentGlobalStationId++;
			findStationById(currentGlobalStationId);
			toPublicStationInfoOnScreen();
			whereAmI();
			toPublicGsmOnScreen("");
			break;
		}

		
		case R.id.button1 : { // фиксация данных 
			
			String message = "Данные зафиксированы ";
			textSomethingChanges = "";
			countOfVibro = 0;
				try {
					createFileInHiddenArea(justTack);
					copyFileFromHiddenToSharedArea(justTack);
					message = message + " успешно";
				} catch (Exception e) {
					// TODO Auto-generated catch block
					message = message + " не успешно";
				}
				Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
				toPublicGsmOnScreen("");
			break;
		}
				   

		case R.id.button2 : // exit button
		{
		    
		    prefs.edit().putInt("ru.alxr.CellPhonelocator", currentGlobalStationId).commit();
	 	    System.exit(0);
			break;
		}
		
		
		default : break;
		}// end  of case/switch
	} // end of method



	@Override 
	public boolean onOptionsItemSelected(MenuItem item) {
		findStationById(item.getItemId());
		if (item.getItemId()!=0) toPublicStationInfoOnScreen();
			
		whereAmI();
		toPublicGsmOnScreen("");// обращаемся к  gsm
		
		return true;}
	
  
    
    public void createFileInHiddenArea(String filename)
    { 
    	try {
    		
    		if (filename.equals(fileForLists)){
    			OutputStreamWriter out =
                        new OutputStreamWriter(openFileOutput(filename, MODE_APPEND));
        		
    				for (NeighboringCellInfo info :nci){
        				Date date = new Date(); 
        				out.append(date.toString()+ "\n");
        				out.append("getNetworkType "+ ((Integer)(info.getNetworkType())).toString()+ "\n");
        				out.append("getCid "+ ((Integer)(info.getCid())).toString()+ "\n");
        				out.append("getLac "+ ((Integer)(info.getLac())).toString()+ "\n");
        				out.append("getPsc "+ ((Integer)(info.getPsc())).toString()+ "\n");
        				out.append("getRssi "+ ((Integer)(info.getRssi())).toString()+ "\n");
        			}
    				out.close();
    		}
    		
    		if (filename.equals(justTack)){
    			OutputStreamWriter out =
                        new OutputStreamWriter(openFileOutput(filename, MODE_APPEND));
				{
					Date date = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat();
		            sdf.applyPattern("yy/MM/dd/ kk:mm:ss");
		            
				    out.append(sdf.format(date) +";" +networkOperatorName+ ";"+((Integer)(cid)).toString()+";" +
		            ((Integer)(lac)).toString() + ";"+
		            _getNetworkOperator + ";"+		
		            ((Integer)(_lastSignalStrength)).toString() + ";"+
		            currentGlobalLine+ ";"+ currentGlobalStation+ ";"+ "\n");
				}
				out.close();
		}
    		
    		
    		
    		
    		
    		
            } catch (java.io.IOException e) {tv.append("IOException in createFileInHiddenArea");
         } catch (Exception e){
        	 tv.append("Exception in createFileInHiddenArea");
        	 
         }
    	
    } // end of createFileInHiddenArea()
    
    
    
    public void copyFileFromHiddenToSharedArea(String filename)
    
    { if (isExternalMemoryAccessible()){
    	File path =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File file = new File(path, filename);
            try {
                path.mkdirs();
                InputStream is = openFileInput(filename);
                OutputStream os = new FileOutputStream(file);
                byte[] data = new byte[is.available()];
                is.read(data);
                os.write(data);
                is.close();
                os.close();
            } catch (IOException e) {
                // Unable to create file, likely because external storage is
                // not currently mounted.
                Log.w("ExternalStorage", "Error writing " + file, e);
                tv.append("IOException  -- ");
                }
      
    } else {Toast toast = Toast.makeText(this, "Внешняя память недоступна!", Toast.LENGTH_LONG);
	toast.setGravity(Gravity.CENTER, 0, 0);
	toast.show();
}
           
}
    
    
    
    
    public boolean isExternalMemoryAccessible()
    
    {
    	boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        boolean accessToRWCard = false;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) 
           {
           // We can read and write the media
           mExternalStorageAvailable = mExternalStorageWriteable = true;
           accessToRWCard = true;
           } 
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))   
           {
           // We can only read the media
           mExternalStorageAvailable = true;
           mExternalStorageWriteable = false;
           } 
        else {mExternalStorageAvailable = mExternalStorageWriteable = false;}
        return accessToRWCard;
    }
}    
    class SimpleStation{
    	int id;
    	String line;
        String station;
        public SimpleStation(){
        	this.line = "";
        	this.station = "";
        }
        public SimpleStation(String _line){
        	this.line = _line;
        	this.station = "";
        	}
        public SimpleStation (String _line, String _station){
        	this.line = _line;
        	this.station = _station;
        }
    	
    }

    
    class SimpleLine {
    	LinkedList <SimpleStation> stations;
    	String lineName;

    	public SimpleLine(String _lineName){
    		this.lineName = _lineName;
    		this.stations = new LinkedList <SimpleStation>();
    	}
    	public SimpleLine(String _lineName, LinkedList <SimpleStation> _stations)
    	{
    		lineName = _lineName;
    		stations = _stations;
    	}
    	public String getLineName(){
    		return this.lineName;
    	}
    	public LinkedList <SimpleStation> getStations (){
    		return this.stations;
    	}

}
    

class MetroData {
	LinkedList <SimpleLine> simpleBase = new LinkedList <SimpleLine> ();
	String s = "Сокольническая;Улица Подбельского;"
			+"Сокольническая;Черкизовская;"
			+"Сокольническая;Преображенская площадь;"
			+"Сокольническая;Сокольники;"
			+"Сокольническая;Красносельская;"
			+"Сокольническая;Комсомольская;"
			+"Сокольническая;Красные ворота;"
			+"Сокольническая;Чистые пруды;"
			+"Сокольническая;Лубянка;"
			+"Сокольническая;Охотный ряд;"
			+"Сокольническая;Библиотека им. Ленина;"
			+"Сокольническая;Кропоткинская;"
			+"Сокольническая;Парк культуры;"
			+"Сокольническая;Фрунзенская;"
			+"Сокольническая;Спортивная;"
			+"Сокольническая;Воробьёвы горы;"
			+"Сокольническая;Университет;"
			+"Сокольническая;Проспект Вернадского;"
			+"Сокольническая;Юго-Западная;"
			+"Замоскворецкая;Алма-Атинская;"
			+"Замоскворецкая;Красногвардейская;"
			+"Замоскворецкая;Домодедовская;"
			+"Замоскворецкая;Орехово;"
			+"Замоскворецкая;Царицыно;"
			+"Замоскворецкая;Кантемировская;"
			+"Замоскворецкая;Каширская;"
			+"Замоскворецкая;Коломенская;"
			+"Замоскворецкая;Автозаводская;"
			+"Замоскворецкая;Павелецкая;"
			+"Замоскворецкая;Новокузнецкая;"
			+"Замоскворецкая;Театральная;"
			+"Замоскворецкая;Тверская;"
			+"Замоскворецкая;Маяковская;"
			+"Замоскворецкая;Белорусская;"
			+"Замоскворецкая;Динамо;"
			+"Замоскворецкая;Аэропорт;"
			+"Замоскворецкая;Сокол;"
			+"Замоскворецкая;Войковская;"
			+"Замоскворецкая;Водный стадион;"
			+"Замоскворецкая;Речной вокзал;"
			+"Арбатско-Покровская;Митино;"
			+"Арбатско-Покровская;Волоколамская;"
			+"Арбатско-Покровская;Мякинино;"
			+"Арбатско-Покровская;Строгино;"
			+"Арбатско-Покровская;Крылатское;"
			+"Арбатско-Покровская;Молодежная;"
			+"Арбатско-Покровская;Кунцевская;"
			+"Арбатско-Покровская;Славянский бульвар;"
			+"Арбатско-Покровская;Парк Победы;"
			+"Арбатско-Покровская;Киевская;"
			+"Арбатско-Покровская;Смоленская глуб.;"
			+"Арбатско-Покровская;Арбатская глуб.;"
			+"Арбатско-Покровская;Площадь Революции;"
			+"Арбатско-Покровская;Курская;"
			+"Арбатско-Покровская;Бауманская;"
			+"Арбатско-Покровская;Электрозаводская;"
			+"Арбатско-Покровская;Семеновская;"
			+"Арбатско-Покровская;Партизанская;"
			+"Арбатско-Покровская;Измайловская;"
			+"Арбатско-Покровская;Первомайская;"
			+"Арбатско-Покровская;Щелковская;"
			+"Филевская;Александровский сад;"
			+"Филевская;Арбатская мелк.;"
			+"Филевская;Смоленская мелк.;"
			+"Филевская;Киевская мелк.;"
			+"Филевская;Выставочная;"
			+"Филевская;Международная;"
			+"Филевская;Студенческая;"
			+"Филевская;Кутузовская;"
			+"Филевская;Фили;"
			+"Филевская;Багратионовская;"
			+"Филевская;Филевский парк;"
			+"Филевская;Пионерская;"
			+"Филевская;Кунцевская;"
			+"Кольцевая;Белорусская;"
			+"Кольцевая;Новослободская;"
			+"Кольцевая;Проспект Мира;"
			+"Кольцевая;Комсомольская;"
			+"Кольцевая;Курская;"
			+"Кольцевая;Таганская;"
			+"Кольцевая;Павелецкая;"
			+"Кольцевая;Добрынинская;"
			+"Кольцевая;Октябрьская;"
			+"Кольцевая;Парк культуры;"
			+"Кольцевая;Киевская;"
			+"Кольцевая;Краснопресненская;"
			+"Калужско-Рижская;Медведково;"
			+"Калужско-Рижская;Бабушкинская;"
			+"Калужско-Рижская;Свиблово;"
			+"Калужско-Рижская;Ботанический сад;"
			+"Калужско-Рижская;ВДНХ;"
			+"Калужско-Рижская;Алексеевская;"
			+"Калужско-Рижская;Рижская;"
			+"Калужско-Рижская;Проспект Мира;"
			+"Калужско-Рижская;Сухаревская;"
			+"Калужско-Рижская;Тургеневская;"
			+"Калужско-Рижская;Китай-город;"
			+"Калужско-Рижская;Третьяковская;"
			+"Калужско-Рижская;Октябрьская;"
			+"Калужско-Рижская;Шаболовская;"
			+"Калужско-Рижская;Ленинский проспект;"
			+"Калужско-Рижская;Академическая;"
			+"Калужско-Рижская;Профсоюзная;"
			+"Калужско-Рижская;Новые Черемушки;"
			+"Калужско-Рижская;Калужская;"
			+"Калужско-Рижская;Беляево;"
			+"Калужско-Рижская;Коньково;"
			+"Калужско-Рижская;Теплый Стан;"
			+"Калужско-Рижская;Ясенево;"
			+"Калужско-Рижская;Новоясеневская;"
			+"Таганско-Краснопресненская;Выхино;"
			+"Таганско-Краснопресненская;Рязанский проспект;"
			+"Таганско-Краснопресненская;Кузьминки;"
			+"Таганско-Краснопресненская;Текстильщики;"
			+"Таганско-Краснопресненская;Волгоградский проспект;"
			+"Таганско-Краснопресненская;Пролетарская;"
			+"Таганско-Краснопресненская;Таганская;"
			+"Таганско-Краснопресненская;Китай-город;"
			+"Таганско-Краснопресненская;Кузнецкий мост;"
			+"Таганско-Краснопресненская;Пушкинская;"
			+"Таганско-Краснопресненская;Баррикадная;"
			+"Таганско-Краснопресненская;Улица 1905 года;"
			+"Таганско-Краснопресненская;Беговая;"
			+"Таганско-Краснопресненская;Полежаевская;"
			+"Таганско-Краснопресненская;Октябрьское поле;"
			+"Таганско-Краснопресненская;Щукинская;"
			+"Таганско-Краснопресненская;Тушинская;"
			+"Таганско-Краснопресненская;Сходненская;"
			+"Таганско-Краснопресненская;Планерная;"
			+"Калининская;Новокосино;"
			+"Калининская;Новогиреево;"
			+"Калининская;Перово;"
			+"Калининская;Шоссе Энтузиастов;"
			+"Калининская;Авиамоторная;"
			+"Калининская;Площадь Ильича;"
			+"Калининская;Марксистская;"
			+"Калининская;Третьяковская;"
			+"Серпуховско-Тимирязевская;Алтуфьево;"
			+"Серпуховско-Тимирязевская;Бибирево;"
			+"Серпуховско-Тимирязевская;Отрадное;"
			+"Серпуховско-Тимирязевская;Владыкино;"
			+"Серпуховско-Тимирязевская;Петровско-Разумовская;"
			+"Серпуховско-Тимирязевская;Тимирязевская;"
			+"Серпуховско-Тимирязевская;Дмитровская;"
			+"Серпуховско-Тимирязевская;Савеловская;"
			+"Серпуховско-Тимирязевская;Менделеевская;"
			+"Серпуховско-Тимирязевская;Цветной бульвар;"
			+"Серпуховско-Тимирязевская;Чеховская;"
			+"Серпуховско-Тимирязевская;Боровицкая;"
			+"Серпуховско-Тимирязевская;Полянка;"
			+"Серпуховско-Тимирязевская;Серпуховская;"
			+"Серпуховско-Тимирязевская;Тульская;"
			+"Серпуховско-Тимирязевская;Нагатинская;"
			+"Серпуховско-Тимирязевская;Нагорная;"
			+"Серпуховско-Тимирязевская;Нахимовский проспект;"
			+"Серпуховско-Тимирязевская;Севастопольская;"
			+"Серпуховско-Тимирязевская;Чертановская;"
			+"Серпуховско-Тимирязевская;Южная;"
			+"Серпуховско-Тимирязевская;Пражская;"
			+"Серпуховско-Тимирязевская;Улица Академика Янгеля;"
			+"Серпуховско-Тимирязевская;Аннино;"
			+"Серпуховско-Тимирязевская;Бульвар Дмитрия Донского;"
			+"Люблинско-Дмитровская;Марьина Роща;"
			+"Люблинско-Дмитровская;Достоевская;"
			+"Люблинско-Дмитровская;Трубная;"
			+"Люблинско-Дмитровская;Сретенский бульвар;"
			+"Люблинско-Дмитровская;Чкаловская;"
			+"Люблинско-Дмитровская;Римская;"
			+"Люблинско-Дмитровская;Крестьянская застава;"
			+"Люблинско-Дмитровская;Дубровка;"
			+"Люблинско-Дмитровская;Кожуховская;"
			+"Люблинско-Дмитровская;Печатники;"
			+"Люблинско-Дмитровская;Волжская;"
			+"Люблинско-Дмитровская;Люблино;"
			+"Люблинско-Дмитровская;Братиславская;"
			+"Люблинско-Дмитровская;Марьино;"
			+"Люблинско-Дмитровская;Борисово;"
			+"Люблинско-Дмитровская;Шипиловская;"
			+"Люблинско-Дмитровская;Зябликово;"
			+"Каховская;Каширская;"
			+"Каховская;Варшавская;"
			+"Каховская;Каховская;"
			+"Бутовская;Битцевский парк;"
			+"Бутовская;Лесопарковая;"
			+"Бутовская;Улица Старокачаловская;"
			+"Бутовская;Улица Скобелевская;"
			+"Бутовская;Бульвар адмирала Ушакова;"
			+"Бутовская;Улица Горчакова;"
			+"Бутовская;Бунинская аллея";
	
	public MetroData (){
		int count=0;
		LinkedList <String> lines = new LinkedList <String> ();
		for (String ss : s.split(";")){
			if (    !(ss.equals("")) && (count%2==0)   ){
				
				SimpleLine sl = new SimpleLine(ss);
				if (!(lines.contains(ss)))  {lines.add(ss);simpleBase.add(sl);}				
			}
			count++;
		}
		count=0;
		for (String ss : s.split(";")){
			if (    !(ss.equals("")) && (count%2==0) &&  !(s.split(";")[count+1]).equals("") ){
				for (SimpleLine sl : simpleBase){
					if(sl.getLineName().equals(ss)){
						sl.stations.add(new SimpleStation(ss, s.split(";")[count+1]));
					}
				}
			}
			count++;
		}
		count = 101;
		for (SimpleLine sl : simpleBase)
			for (SimpleStation sst : sl.stations) sst.id = count++;
	}
}