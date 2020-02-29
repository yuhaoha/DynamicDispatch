package staticRedDispatch;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class UserUtil{
	public static List<User> users = new ArrayList<User>(); //�û��б�
	
	// ��ȡ�û�����
	public static void readUserData() throws Exception {
		// ��ȡExcel�ĵ�����
		XSSFWorkbook xssfWorkbook = new XSSFWorkbook(
				new FileInputStream("file/�û�����.xlsx"));
		// ��ȡҪ�����ı�񣨵�һ�����
		XSSFSheet sheet = xssfWorkbook.getSheetAt(0);
		// ѭ����ȡÿ������
		for(Row row:sheet)
		{
			if(row.getRowNum()==0)
				continue;
			int userId = (int) row.getCell(0).getNumericCellValue();
			int gender = (int) row.getCell(3).getNumericCellValue();
			int consumption = (int) row.getCell(4).getNumericCellValue();
			int otherVehicle = (int) row.getCell(5).getNumericCellValue();
			int frequency = (int) row.getCell(6).getNumericCellValue();
			int urgency = (int) row.getCell(7).getNumericCellValue();
			User user = new User(userId, gender, consumption, otherVehicle, frequency, urgency);
			users.add(user);
			// ����money��С������������
			MyComparator mComparator = new MyComparator();
			Collections.sort(users,mComparator);
		}
		xssfWorkbook.close();
	}
	
	// ���ݱ�����ȡ���
	public static double getInspireMoney(double rate)
	{
		try {
			readUserData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		int userSize = users.size();
		int pos = (int)(rate * userSize)-1;
		return users.get(pos).money;
	}
	
	// ������������Ҫ�Ļ���
	public static double countStimulateCost(int personNum)
	{
		if(personNum<=0)
			return 0.0;
		try {
			readUserData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		double stimulateCost = 0;
		for(int i=0;i<personNum;i++)
		{
			stimulateCost += users.get(i).money;
		}
		return stimulateCost;
	}
	
	public static void main(String[] args) throws Exception {
		 readUserData();
//		for(int i=0;i<users.size();i++)
//		{
//
//			System.out.println(i+1+"  "+users.get(i));
//		}
	}
}


class User {
	int userId;
	int distance;
	int gender;
	int consumption;
	int otherVehicle;
	int frequency;
	int urgency;
	double money=0;  // ��ʹ�û�ȥ�ｩʬ���ĺ�����

	public User(int userId, int gender, int consumption, int otherVehicle, int frequency, int urgency) {
		this.userId = userId;
		this.gender = gender;
		this.consumption = consumption;
		this.otherVehicle = otherVehicle;
		this.frequency = frequency;
		this.urgency = urgency;
		Random random = new Random();
		this.distance = random.nextInt(500);
		// ����money
		if(distance<=300)
			money = 0.0205*distance+1.719*Math.log(consumption)+0.743*urgency+0.0306*gender-0.4*otherVehicle-0.101*frequency-13.28;
		else 
			money = 0.0382*distance+4.692*Math.log(consumption)+1.271*urgency+0.29*gender-0.813*otherVehicle+0.0199*frequency-43.13;
		// ���ж��⴦��
		if(money<0)
			money = 0;
		money = money/10+0.5;
	}

	@Override
	public String toString() {
		return "User [userId=" + userId + ", distance=" + distance + ", gender=" + gender + ", consumption="
				+ consumption + ", otherVehicle=" + otherVehicle + ", frequency=" + frequency + ", urgency=" + urgency
				+ ", money=" + money + "]";
	}
}

class MyComparator implements Comparator<User>{
	@Override
	public int compare(User u1, User u2) {
		if(u1.money < u2.money)
			return -1;
		else if(u1.money == u2.money)
			return 0;
		else
			return 1;
	}

}

// 02.25 update
// maxCapacity = 80 manageTime =0.2
