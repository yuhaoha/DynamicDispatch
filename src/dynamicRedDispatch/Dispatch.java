package dynamicRedDispatch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;


public class Dispatch {

	class Car extends Thread {
		int id; // �������,��Ӧh
		double fixedCost = 100; // �̶��ɱ� E �ȼ���100��
		double perCost = 2.21; // ��ʻ��λ����ɱ� F
		int maxCapacity = 52; // ���� G
		int capacity = 0; // ʵ��װ�ص�����
		double maxRunDistance = 150; // �����ʻ���� L
		double runDistance = 0; // ʵ����ʻ����
		double workTime = 120; // �����˹���ʱ�� T ʱ�䵥λ����min
		double workPerHour = 38.0 / 60.0; // ������ÿ��������
		double manageTime = 0.2; // ���ա�����һ��������ʱ�� t
		int stopAndStart = 2; // ���ȳ�ͣ��������������������ʱ��
		int loadCars; // ���ȳ��ӵ������ĳ���ʱװ�е���õ����� K
		double speed = 35; // ���ȳ�ƽ����ʻ�ٶ� km/h
		ArrayList<Operation> path = new ArrayList<Operation>(); // ��¼�ó�·��
		int currentArea = 0; // ��ǰλ��(ǰ��+����)
		int currentTime = 0; // ��ǰʱ�䣬�����������ѹ���ʱ��
		private final CountDownLatch countDownLatch;
		// ���캯�� ����������������������Ϊ����
		Car(int id, int loadCars,CountDownLatch countDownLatch) {
			this.id = id;
			this.loadCars = loadCars;
			this.capacity = loadCars;
			this.countDownLatch = countDownLatch;
		}

		@Override
		public void run() // �̺߳��������е��ȳ���һЩ����
		{
			try {
				countDownLatch.await();
				// System.out.println(Thread.currentThread().getName() + "����ʱ����" + System.currentTimeMillis());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				sleep(this.id);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			while (currentTime <= 120) {
				int oldTime = currentTime;
				int carNextArea = findNextArea(); // �ҵ���������
				int completeTime;
				if(carNextArea == -1)
				{
					carNextArea = 0;
					completeTime = gotoCenter();
				}
				else
					 completeTime = completeMission(carNextArea); // ��������ɺ��ʱ��
				try {
					sleep((completeTime-oldTime)*100);
//					System.out.println("���䳵"+id+"����"+carNextArea+" ʱ��:"+completeTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// �������󣬽�����������ɵ���ʱ��Ϊ����ʱ��ʱ���Ÿ���
				updateNeed(completeTime);
				if(hasCompleted()) //�߹����е�
					break;
			}

		}
		
		boolean overDistance() // �ж��Ƿ񳬹����о���,û��������true
		{
			return maxRunDistance >= runDistance;
		}

		/* ���������䳵��ĳ��������еĲ��� */

		
		// �Ծ��������Ϊԭ���ҵ���һ��ǰ�������򣨳�ȥ�Լ�������������ǰ��������
		int findNextArea() {
			double bestIncome = -999999; // ���������
			int pos = -1;  //Ӧ����-1
			for (Area a : areas) {
				if(visited[this.id][a.id])
					continue;
				int d = a.currentDispatch.get(id); //������
				if(d==0)
					continue;
				if (currentArea == a.id) // ��ǰ���ڵ��ȵ���a��ͬ
					continue;
				boolean judge = false; // �ж��Ƿ�����������Ŀ�ĵ�
				for (int k : gcurrentArea) {
					if (k == a.id) {
						judge = true;
						break;
					}
				}
				if (judge)
					continue;
				double distanceCost = distanceMap[currentArea][a.id] * perCost; // ���ȵķ���
				double bikeIncome=0.0 ; // ���Ⱥ�ɻ�õ�����
				int r = 0; // ʵ�ʵ�����
				if (a.currentDispatch.get(id) > 0) // Ͷ��
				{
					if (capacity > a.currentDispatch.get(id))
						r = a.currentDispatch.get(id);
					else
						r = capacity;
				} else if (a.currentDispatch.get(id) < 0) // ����
				{
					if (maxCapacity - capacity > -a.currentDispatch.get(id))
						r = a.currentDispatch.get(id);
					else
						r =  capacity - maxCapacity;
				}
				if(r==0)
				{
					continue;
				}
				bikeIncome = 0.47*Math.abs(r)*1;
				double monthCard = 0.53*Math.abs(d)*coefficient*(countProbability(r, d, 5)+countProbability(r, d, 6)+countProbability(r, d, 7))/7;
				// monthCard = 0.0;  //��������
				double income = bikeIncome - monthCard-distanceCost;
				if (income > bestIncome) {
					bestIncome = income;
					pos = a.id;
				}
			}
			if(pos == -1)
			{
				return pos;
			}
//			double monthCard = 0.53*Math.abs(dd)*10*(countProbability(rr, dd, 5)+countProbability(rr, dd, 6)+countProbability(rr, dd, 7))/7;
//			System.out.println("���棺"+ 0.47*Math.abs(rr)+" �¿���ʧ:"+monthCard+" �������:"+dc);
			return pos;
		}

		// û����ȥ�ĵ�:���ȳ���û�е������Ҹ�������Ϊ������capacity==0���������Ҹ�������Ϊ������capacity==52�����ص������ġ������ɸ���һ���³�
		int gotoCenter()
		{
			// ��ʻ����������
			runDistance += distanceMap[currentArea][0];
			int runTime = (int) (distanceMap[currentArea][0] / speed * 60); // ��ʻʱ��
			currentArea = 0;
			gcurrentArea.set(id, 0);
			if(capacity == 0) //��Ҫװ�ص���
			{
				capacity = 30;  //װ��30��
			}
			else  //��Ҫ���õ���
			{
				capacity = 0;  //ж�����г�
			}
			currentTime = currentTime + runTime + stopAndStart; // ���±����ӽǵĵ�ǰʱ��
			gcurrentTime.set(id, currentTime); // ȫ�ֱ�������ͬ��
			// �������䳵�ڸ�����Ĳ����洢զOperation����o�У����뵽�ó���·������path��
			Operation o = new Operation(id, 0, 0, 0, currentTime); //��������ʱ��
			path.add(o);
			// System.out.println(o);
			return currentTime; // ���ص�ǰʱ�䣬�������������µ���ֵ
		}
		
		// ǰ�����ȵ㲢��ɵ�������
		int completeMission(int pos) {
			// ��ʻ����һ����
			runDistance += distanceMap[currentArea][pos];
			// System.out.println(currentArea+"��"+pos+"��ʻ���룺"+distanceMap[currentArea][pos]);
			int runTime = (int) (distanceMap[currentArea][pos] / speed * 60); // ��ʻʱ��
			currentArea = pos;
			gcurrentArea.set(id, pos);
			Area a = areas.get(pos);
			int r = 0; // ʵ�ʵ�����
			if (a.currentDispatch.get(id) > 0) // Ͷ��
			{
				if (capacity > a.currentDispatch.get(id))
					r = a.currentDispatch.get(id);
				else
					r = capacity;
			} else if (a.currentDispatch.get(id) < 0) // ����
			{
				if (maxCapacity - capacity > -a.currentDispatch.get(id))
					r = a.currentDispatch.get(id);
				else
					r = capacity - maxCapacity;
			}
			capacity = capacity - r; // ʵ�������ı�
			a.realDispatch += r; // ��ʵ����ֵ�ı�
			visited[this.id][a.id] = true;
			currentTime = currentTime + runTime + stopAndStart + (int) (Math.abs(r) * manageTime); // ���±����ӽǵĵ�ǰʱ��
			a.currentTime = currentTime; // �������һ�α������ʱ��
			gcurrentTime.set(id, currentTime); // ȫ�ֱ�������ͬ��
			// �������䳵�ڸ�����Ĳ����洢զOperation����o�У����뵽�ó���·������path��
			Operation o = new Operation(id, pos, a.currentDispatch.get(id),r, currentTime); //��������ʱ��
			path.add(o);
			// System.out.println(o);
			return currentTime; // ���ص�ǰʱ�䣬�������������µ���ֵ
		}
		
		synchronized void updateNeed(int completeTime)
		{
			boolean update = true;
//			System.out.println("��ʱ��"+completeTime+"��������");
//			for (int k : gcurrentTime) {
//				if (k > completeTime) {
//					update = false;
//					break;
//				}
//			}
			if (update) {
				if(completeTime>119) //����ʱ��
					return;
				for (Area a : areas) {
					boolean judge = false; // �ж��Ƿ�����������Ŀ�ĵģ������ ����������
					for (int k : gcurrentArea) {
						if (k == a.id&&a.id!=currentArea) {
							judge = true;
							break;
						}
					}
					if(!judge)
						a.currentDispatch.set(id,a.getDemand(completeTime));
				}
			}
		}
		
		// �ж��Ƿ��߹����е�
		boolean hasCompleted()
		{
			boolean judge = true;
			for(int i=0;i<areaNum;i++)
			{
				if(!visited[this.id][i])
				{
					judge = false;
					break;
				}
			}
			return judge;
		}
	}

	class Area {
		// boolean visited = false; //�Ƿ񱻷��ʹ�
		int id; // ���������ţ�0Ϊ��������,��Ӧi
		String name; // ����
		int dispatch; // �������� ����ֵ
		// int currentDispatch = 0; // δ��20����������
		List<Integer> currentDispatch = new ArrayList<Integer>(); //ÿ�������Լ��ĵ�ǰ�������currentDispatch.get(i)
		int realDispatch = 0; // ÿ������ʵ�ʵõ��ĵ���
		// ��ǰʱ�䣬ȫ�ֱ��� 0-120
		int currentTime = 0; // �������һ�α������ʱ��
		int redBike = 0;
		int redBikeNeed = 0;  //����������������
		// ���캯��
		Area(int id ,String name, int dispatch,int currentDispatch,int redBike) {
			this.id = id;
			if(currentDispatch>0) //�õ�����������
			{
				redBikeNeed = (int)(redBike*stimulateRate);
				if(redBikeNeed > currentDispatch) //����������������
					redBikeNeed = currentDispatch;
				realDispatch = redBikeNeed;
				currentDispatch -= redBikeNeed;
				// System.out.println("����"+id+"�ڱ���"+stimulateRate+"�£������������������Ϊ��"+redBikeNeed);
			}
			for(int i=0;i<carNum;i++)
			{
				this.currentDispatch.add(currentDispatch);
			}
			// this.currentDispatch = currentDispatch;
			this.dispatch = dispatch;
			this.name = name;
		}

		// ���뵱ǰʱ�䣬������ʵ����
		int getDemand(int time) {
			int realNeed = areaNeed[id] ; //����������Ҫ���Ǽ�ȥrealDispatch��ֻ����������һ��Ҳ����Ҫ����
			return realNeed;
		}
	}

	class Operation {
		// h��ÿͣ��һ�Σ�����һ�����󣬴��ڶ�Ӧ��ArrayList��
		int carId; // ��Ӧh
		int areaId; // ��Ӧi
		int currentNeed; //��ʱ������
		int place; // h��������i����/���յĵ����� ����Ϊ��ֵ
		int time; // ��ɱ��ε���ʱ��
		// ���캯��

		Operation(int carId, int areaId, int currentNeed,int place, int time) {
			this.carId = carId;
			this.areaId = areaId;
			this.currentNeed = currentNeed;
			this.place = place;
			this.time = time;
		}

		@Override
		public String toString() {
			return "Operation [�����=" + carId + ", ������=" + areaId + ", ʵʱ����=" + currentNeed + ", ʵ������="
					+ place + ", ���ʱ��=" + time + "]";
		}
		
	}

	public int coefficient;   // �¿��ͷ�ϵ��
	public double stimulateRate = 0.0; //����ʹ�ú�����ĸ��ʣ���0.1 ��10%�ĺ������ʹ�ã���Ӧ�����������10%�û��������ȳ�����������Ҫ��ȥ�����������
	public static int carNum; // ���䳵����
	public List<Car> cars = new ArrayList<Car>(); // ���䳵����
	public int areaNum;
	public  List<Area> areas = new ArrayList<Area>(); // ��������
	// ��������֮�����Ķ�ά���󣬴�СΪareaNum*areaNum
	public double[][] distanceMap = { 
			{ 0, 6.8, 2.1, 2, 1.3, 6.5, 1.3, 1.9, 3.1, 1.3, 1.5, 1.6 },
			{ 6.8, 0, 5.2, 9.5, 5.5, 1.7, 7.2, 5.1, 3.8, 7, 8.4, 8.3 },
			{ 2.1, 5.2, 0, 4.6, 1.6, 5.2, 3.3, 1.8, 1.3, 1.8, 3.5, 3.3 },
			{ 2, 9.5, 4.6, 0, 4, 9, 2.6, 4.3, 5.7, 3, 1.1, 1.5 },
			{ 1.3, 5.5, 1.6, 4, 0, 5.1, 1.9, 0.49, 2.1, 2, 2.9, 2.9 },
			{ 6.5, 1.7, 5.2, 9, 5.1, 0, 6.6, 4.7, 4, 6.9, 8.1, 8 },
			{ 1.3, 7.2, 3.3, 2.6, 1.9, 6.6, 0, 2.2, 4, 2.6, 2, 7.7 },
			{ 1.9, 5.1, 1.8, 4.3, 0.49, 4.7, 2.2, 0, 1.9, 2.5, 3.4, 3.4 },
			{ 3.1, 3.8, 1.3, 5.7, 2.1, 4, 4, 1.9, 0, 3.1, 4.6, 4.4 },
			{ 1.3, 7, 1.8, 3, 2, 6.9, 2.6, 2.5, 3.1, 0, 1.9, 1.5 },
			{ 1.5, 8.4, 3.5, 1.1, 2.9, 8.1, 2, 3.4, 4.6, 1.9, 0, 0.49 },
			{ 1.6, 8.3, 3.3, 1.5, 2.9, 8, 7.7, 3.4, 4.4, 1.5, 0.49, 0 } };
	public int [] areaNeed = new int[12];  //��������ÿ��ʱ�̵�����
	public List<Integer> gcurrentArea = new ArrayList<Integer>(); // ÿ������һ��Ҫǰ���ĵص�
	public List<Integer> gcurrentTime = new ArrayList<Integer>(); // ÿ��������ʱ��
	public boolean visited[][]; //ÿ�����Ƿ�ȥ�����ȵ�i
	
	// ��ʼ������
	void initNeed()
	{
		for(Area area:areas)
		{
			areaNeed[area.id] = area.currentDispatch.get(0);
		}
	}
	
	// �������
	int fact(int num)
	{
		int sum=1;
		for(int i=1;i<=num;i++)
			sum*=i;
		return sum;
	}
	
	double countProbability(int r, int d,int i)
	{
		double s = ((double)r)/((double)d);
		int num = fact(7)/(fact(i)*fact(7-i));
		double result = num*Math.pow(1-s, i)*Math.pow(s, 7-i);
		return result;
	}
	// ����ɱ�
	public double countCost()
	{
		double totalCost = 0;
		double distanceCost=0;  //��ʻ�ɱ�
		for(Car car:cars)
			distanceCost += car.runDistance * car.perCost;
		double timeCost = 0;  //ʱ��ɱ�
		for(Car car:cars)
		{
			int workTime  = car.currentTime;
			timeCost += workTime*cars.get(0).workPerHour;
		}
		double punishCost =0; //�ͷ��ɱ�
		for(Area area: areas)
		{
			int r = area.realDispatch;
			int d = area.dispatch;
			double temp = 0.47*Math.abs(d-r)*1+0.53*Math.abs(d)*coefficient*(countProbability(r, d, 5)+countProbability(r, d, 6)+countProbability(r, d, 7))/7;
			punishCost += temp;
			punishCost -= area.redBikeNeed*0.47;  //���ⲹ��
//			System.out.println("����"+area.id+"����:"+d+" ����:"+r+" �ͷ��ɱ�:"+temp);
		}
		double stimulateCost =0.0; //�����û�ʹ�ú�����ĳɱ�
		int useRedBikeNum = 0; //ʹ�õĺ��������
		for(Area area:areas)
		{
			if(area.redBikeNeed>0)
			{
				double onceCost = UserUtil.countStimulateCost(area.redBikeNeed);
				stimulateCost += onceCost;
				useRedBikeNum += area.redBikeNeed;
			}
		}
		System.out.println("ʹ�ú������������"+useRedBikeNum);
		totalCost = distanceCost + timeCost + punishCost+stimulateCost;
		System.out.println("��ʻ�ɱ�="+distanceCost+" ʱ��ɱ�="+timeCost+" �ͷ��ɱ�="+punishCost+ " ��������ɱ�="+stimulateCost+ " �ܳɱ�="+totalCost);
		return totalCost;
	}
	
	public static void runWithRedBike(double rate)
	{
		Dispatch dispatch = new Dispatch();
		dispatch.areaNum = 12;
		dispatch.coefficient = 10;
		Dispatch.carNum = 1;
		int initCarNum = 10;
		dispatch.stimulateRate = rate;
		System.out.println("������="+dispatch.stimulateRate+"ʱ�� �������="+UserUtil.getInspireMoney(dispatch.stimulateRate));
		dispatch.areas.add(dispatch.new Area(0, "Զ��δ���㹺������", 28,23,77));
		dispatch.areas.add(dispatch.new Area(1, "�����г�����������98��", -106,-101,116));
		dispatch.areas.add(dispatch.new Area(2, "����ԷC��", -63,-55,111));
		dispatch.areas.add(dispatch.new Area(3, "����¥����վ", 39,31,80));
		dispatch.areas.add(dispatch.new Area(4, "����ׯ·����ͥ�Ƶ꣩", -29,-21,75));
		dispatch.areas.add(dispatch.new Area(5, "����������ׯ��һСѧ", -72,-64,57));
		dispatch.areas.add(dispatch.new Area(6, "�����԰", -81,-73,58));
		dispatch.areas.add(dispatch.new Area(7, "��ݼԺС��", 43,35,51));
		dispatch.areas.add(dispatch.new Area(8, "�߱����´�С��", 50,44,55));
		dispatch.areas.add(dispatch.new Area(9, "ƽ����С����", 49,44,141));
		dispatch.areas.add(dispatch.new Area(10, "�����ձ���", -31,-22,87));
		dispatch.areas.add(dispatch.new Area(11, "�׶�����ó�״�ѧ", 30,23,47));
		dispatch.initNeed();
		dispatch.visited = new boolean[Dispatch.carNum][dispatch.areaNum];
		for(int i=0;i<Dispatch.carNum;i++)
			for(int j=0;j<dispatch.areaNum;j++)
				dispatch.visited[i][j] = false;
		// �Գ���ʼ��
		CountDownLatch countDownLatch = new CountDownLatch(carNum);
		for (int i = 0; i < Dispatch.carNum; i++) {
//			System.out.println("�����߳�"+i);
			dispatch.gcurrentArea.add(0);
			dispatch.gcurrentTime.add(0);
			Car car = dispatch.new Car(i, initCarNum,countDownLatch); // ��ʼЯ��10������
			dispatch.cars.add(car);
			car.start();
			countDownLatch.countDown();
		}
		//�ȴ����߳̽���
		for(Car car: dispatch.cars)
		{
			try {
				car.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		for(Car car:dispatch.cars)
		{
//			System.out.println("*****��"+car.id+"�����������У�·������*****");
			for(Operation o : car.path)
			{
				// if(o.currentNeed!=0)
				System.out.println(o);
			}
		}
		dispatch.countCost();
		System.out.println("\n\n");
	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
		runWithRedBike(0.5);
	}
	
}
