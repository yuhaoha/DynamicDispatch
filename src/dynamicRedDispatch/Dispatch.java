package dynamicRedDispatch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;


public class Dispatch {

	class Car extends Thread {
		int id; // 车辆编号,对应h
		double fixedCost = 100; // 固定成本 E 先假设100吧
		double perCost = 2.21; // 行驶单位距离成本 F
		int maxCapacity = 52; // 容量 G
		int capacity = 0; // 实际装载的容量
		double maxRunDistance = 150; // 最大行驶距离 L
		double runDistance = 0; // 实际行驶距离
		double workTime = 120; // 调度人工作时间 T 时间单位都是min
		double workPerHour = 38.0 / 60.0; // 调度人每分钟收入
		double manageTime = 0.2; // 回收、放置一辆单车的时间 t
		int stopAndStart = 2; // 调度车停靠和启动两个过程所需时间
		int loadCars; // 调度车从调度中心出发时装有的完好单车数 K
		double speed = 35; // 调度车平均行驶速度 km/h
		ArrayList<Operation> path = new ArrayList<Operation>(); // 记录该车路径
		int currentArea = 0; // 当前位置(前往+工作)
		int currentTime = 0; // 当前时间，可以理解成是已工作时间
		private final CountDownLatch countDownLatch;
		// 构造函数 除了这两个变量外其他均为常量
		Car(int id, int loadCars,CountDownLatch countDownLatch) {
			this.id = id;
			this.loadCars = loadCars;
			this.capacity = loadCars;
			this.countDownLatch = countDownLatch;
		}

		@Override
		public void run() // 线程函数，进行调度车的一些操作
		{
			try {
				countDownLatch.await();
				// System.out.println(Thread.currentThread().getName() + "启动时间是" + System.currentTimeMillis());
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
				int carNextArea = findNextArea(); // 找到调度区域
				int completeTime;
				if(carNextArea == -1)
				{
					carNextArea = 0;
					completeTime = gotoCenter();
				}
				else
					 completeTime = completeMission(carNextArea); // 该区域完成后的时间
				try {
					sleep((completeTime-oldTime)*100);
//					System.out.println("运输车"+id+"到达"+carNextArea+" 时间:"+completeTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// 更新需求，仅当该区域完成调度时间为最新时间时，才更新
				updateNeed(completeTime);
				if(hasCompleted()) //走过所有点
					break;
			}

		}
		
		boolean overDistance() // 判断是否超过运行距离,没超过返回true
		{
			return maxRunDistance >= runDistance;
		}

		/* 以下是运输车在某个区域进行的操作 */

		
		// 以距离和收益为原则，找到下一个前往的区域（除去自己和其他车正在前往的区域）
		int findNextArea() {
			double bestIncome = -999999; // 最佳总收益
			int pos = -1;  //应该是-1
			for (Area a : areas) {
				if(visited[this.id][a.id])
					continue;
				int d = a.currentDispatch.get(id); //需求量
				if(d==0)
					continue;
				if (currentArea == a.id) // 当前所在调度点与a相同
					continue;
				boolean judge = false; // 判断是否是其他车的目的的
				for (int k : gcurrentArea) {
					if (k == a.id) {
						judge = true;
						break;
					}
				}
				if (judge)
					continue;
				double distanceCost = distanceMap[currentArea][a.id] * perCost; // 调度的费用
				double bikeIncome=0.0 ; // 调度后可获得的收益
				int r = 0; // 实际调度量
				if (a.currentDispatch.get(id) > 0) // 投放
				{
					if (capacity > a.currentDispatch.get(id))
						r = a.currentDispatch.get(id);
					else
						r = capacity;
				} else if (a.currentDispatch.get(id) < 0) // 回收
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
				// monthCard = 0.0;  //用来调试
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
//			System.out.println("收益："+ 0.47*Math.abs(rr)+" 月卡损失:"+monthCard+" 距离代价:"+dc);
			return pos;
		}

		// 没有能去的点:调度车上没有单车，且各点需求为正数（capacity==0）。满载且各点需求为负数（capacity==52）返回调度中心。可理解成更换一辆新车
		int gotoCenter()
		{
			// 行驶到调度中心
			runDistance += distanceMap[currentArea][0];
			int runTime = (int) (distanceMap[currentArea][0] / speed * 60); // 行驶时间
			currentArea = 0;
			gcurrentArea.set(id, 0);
			if(capacity == 0) //需要装载单车
			{
				capacity = 30;  //装载30辆
			}
			else  //需要放置单车
			{
				capacity = 0;  //卸掉所有车
			}
			currentTime = currentTime + runTime + stopAndStart; // 更新本车视角的当前时间
			gcurrentTime.set(id, currentTime); // 全局变量用来同步
			// 将该运输车在该区域的操作存储咋Operation对象o中，加入到该车的路径链表path中
			Operation o = new Operation(id, 0, 0, 0, currentTime); //完成任务的时间
			path.add(o);
			// System.out.println(o);
			return currentTime; // 返回当前时间，给各区域来更新调度值
		}
		
		// 前往调度点并完成调度任务
		int completeMission(int pos) {
			// 行驶到下一区域
			runDistance += distanceMap[currentArea][pos];
			// System.out.println(currentArea+"到"+pos+"行驶距离："+distanceMap[currentArea][pos]);
			int runTime = (int) (distanceMap[currentArea][pos] / speed * 60); // 行驶时间
			currentArea = pos;
			gcurrentArea.set(id, pos);
			Area a = areas.get(pos);
			int r = 0; // 实际调度量
			if (a.currentDispatch.get(id) > 0) // 投放
			{
				if (capacity > a.currentDispatch.get(id))
					r = a.currentDispatch.get(id);
				else
					r = capacity;
			} else if (a.currentDispatch.get(id) < 0) // 回收
			{
				if (maxCapacity - capacity > -a.currentDispatch.get(id))
					r = a.currentDispatch.get(id);
				else
					r = capacity - maxCapacity;
			}
			capacity = capacity - r; // 实际容量改变
			a.realDispatch += r; // 真实调度值改变
			visited[this.id][a.id] = true;
			currentTime = currentTime + runTime + stopAndStart + (int) (Math.abs(r) * manageTime); // 更新本车视角的当前时间
			a.currentTime = currentTime; // 区域最后一次被服务的时间
			gcurrentTime.set(id, currentTime); // 全局变量用来同步
			// 将该运输车在该区域的操作存储咋Operation对象o中，加入到该车的路径链表path中
			Operation o = new Operation(id, pos, a.currentDispatch.get(id),r, currentTime); //完成任务的时间
			path.add(o);
			// System.out.println(o);
			return currentTime; // 返回当前时间，给各区域来更新调度值
		}
		
		synchronized void updateNeed(int completeTime)
		{
			boolean update = true;
//			System.out.println("在时间"+completeTime+"更新需求");
//			for (int k : gcurrentTime) {
//				if (k > completeTime) {
//					update = false;
//					break;
//				}
//			}
			if (update) {
				if(completeTime>119) //超出时间
					return;
				for (Area a : areas) {
					boolean judge = false; // 判断是否是其他车的目的的，如果是 不更新需求
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
		
		// 判断是否走过所有点
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
		// boolean visited = false; //是否被访问过
		int id; // 调度区域编号，0为调度中心,对应i
		String name; // 名称
		int dispatch; // 总需求量 调度值
		// int currentDispatch = 0; // 未来20分钟需求量
		List<Integer> currentDispatch = new ArrayList<Integer>(); //每辆车有自己的当前需求表，即currentDispatch.get(i)
		int realDispatch = 0; // 每个区域实际得到的调度
		// 当前时间，全局变量 0-120
		int currentTime = 0; // 区域最后一次被服务的时间
		int redBike = 0;
		int redBikeNeed = 0;  //红包车满足的需求数
		// 构造函数
		Area(int id ,String name, int dispatch,int currentDispatch,int redBike) {
			this.id = id;
			if(currentDispatch>0) //用到红包车的情况
			{
				redBikeNeed = (int)(redBike*stimulateRate);
				if(redBikeNeed > currentDispatch) //满足量大于需求量
					redBikeNeed = currentDispatch;
				realDispatch = redBikeNeed;
				currentDispatch -= redBikeNeed;
				// System.out.println("区域"+id+"在比率"+stimulateRate+"下，红包车可满足需求数为："+redBikeNeed);
			}
			for(int i=0;i<carNum;i++)
			{
				this.currentDispatch.add(currentDispatch);
			}
			// this.currentDispatch = currentDispatch;
			this.dispatch = dispatch;
			this.name = name;
		}

		// 输入当前时间，返回真实需求
		int getDemand(int time) {
			int realNeed = areaNeed[id] ; //单辆车不需要考虑减去realDispatch，只允许经过区域一次也不需要考虑
			return realNeed;
		}
	}

	class Operation {
		// h车每停靠一次，创建一个对象，存在对应的ArrayList中
		int carId; // 对应h
		int areaId; // 对应i
		int currentNeed; //当时的需求
		int place; // h车在区域i放置/回收的单车数 回收为负值
		int time; // 完成本次调度时间
		// 构造函数

		Operation(int carId, int areaId, int currentNeed,int place, int time) {
			this.carId = carId;
			this.areaId = areaId;
			this.currentNeed = currentNeed;
			this.place = place;
			this.time = time;
		}

		@Override
		public String toString() {
			return "Operation [车编号=" + carId + ", 区域编号=" + areaId + ", 实时需求=" + currentNeed + ", 实际满足="
					+ place + ", 完成时间=" + time + "]";
		}
		
	}

	public int coefficient;   // 月卡惩罚系数
	public double stimulateRate = 0.0; //激励使用红包车的概率，如0.1 则10%的红包车被使用（对应最容易满足的10%用户），调度车的需求量需要减去红包车抵消的
	public static int carNum; // 运输车数量
	public List<Car> cars = new ArrayList<Car>(); // 运输车数组
	public int areaNum;
	public  List<Area> areas = new ArrayList<Area>(); // 区域数组
	// 各个区域之间距离的二维矩阵，大小为areaNum*areaNum
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
	public int [] areaNeed = new int[12];  //各区域在每个时刻的需求
	public List<Integer> gcurrentArea = new ArrayList<Integer>(); // 每辆车下一个要前往的地点
	public List<Integer> gcurrentTime = new ArrayList<Integer>(); // 每辆车工作时间
	public boolean visited[][]; //每辆车是否去过调度点i
	
	// 初始化需求
	void initNeed()
	{
		for(Area area:areas)
		{
			areaNeed[area.id] = area.currentDispatch.get(0);
		}
	}
	
	// 计算概率
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
	// 计算成本
	public double countCost()
	{
		double totalCost = 0;
		double distanceCost=0;  //行驶成本
		for(Car car:cars)
			distanceCost += car.runDistance * car.perCost;
		double timeCost = 0;  //时间成本
		for(Car car:cars)
		{
			int workTime  = car.currentTime;
			timeCost += workTime*cars.get(0).workPerHour;
		}
		double punishCost =0; //惩罚成本
		for(Area area: areas)
		{
			int r = area.realDispatch;
			int d = area.dispatch;
			double temp = 0.47*Math.abs(d-r)*1+0.53*Math.abs(d)*coefficient*(countProbability(r, d, 5)+countProbability(r, d, 6)+countProbability(r, d, 7))/7;
			punishCost += temp;
			punishCost -= area.redBikeNeed*0.47;  //额外补充
//			System.out.println("区域"+area.id+"需求:"+d+" 满足:"+r+" 惩罚成本:"+temp);
		}
		double stimulateCost =0.0; //激励用户使用红包车的成本
		int useRedBikeNum = 0; //使用的红包车数量
		for(Area area:areas)
		{
			if(area.redBikeNeed>0)
			{
				double onceCost = UserUtil.countStimulateCost(area.redBikeNeed);
				stimulateCost += onceCost;
				useRedBikeNum += area.redBikeNeed;
			}
		}
		System.out.println("使用红包车总数量："+useRedBikeNum);
		totalCost = distanceCost + timeCost + punishCost+stimulateCost;
		System.out.println("行驶成本="+distanceCost+" 时间成本="+timeCost+" 惩罚成本="+punishCost+ " 红包激励成本="+stimulateCost+ " 总成本="+totalCost);
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
		System.out.println("激励率="+dispatch.stimulateRate+"时， 激励金额="+UserUtil.getInspireMoney(dispatch.stimulateRate));
		dispatch.areas.add(dispatch.new Area(0, "远洋未来汇购物中心", 28,23,77));
		dispatch.areas.add(dispatch.new Area(1, "北京市朝阳区西柳巷98号", -106,-101,116));
		dispatch.areas.add(dispatch.new Area(2, "国粹苑C座", -63,-55,111));
		dispatch.areas.add(dispatch.new Area(3, "呼家楼地铁站", 39,31,80));
		dispatch.areas.add(dispatch.new Area(4, "八里庄路（汉庭酒店）", -29,-21,75));
		dispatch.areas.add(dispatch.new Area(5, "朝阳区定福庄第一小学", -72,-64,57));
		dispatch.areas.add(dispatch.new Area(6, "红领巾公园", -81,-73,58));
		dispatch.areas.add(dispatch.new Area(7, "芳菁院小区", 43,35,51));
		dispatch.areas.add(dispatch.new Area(8, "高碑店新村小区", 50,44,55));
		dispatch.areas.add(dispatch.new Area(9, "平房区小街巷", 49,44,141));
		dispatch.areas.add(dispatch.new Area(10, "人民日报社", -31,-22,87));
		dispatch.areas.add(dispatch.new Area(11, "首都经济贸易大学", 30,23,47));
		dispatch.initNeed();
		dispatch.visited = new boolean[Dispatch.carNum][dispatch.areaNum];
		for(int i=0;i<Dispatch.carNum;i++)
			for(int j=0;j<dispatch.areaNum;j++)
				dispatch.visited[i][j] = false;
		// 对车初始化
		CountDownLatch countDownLatch = new CountDownLatch(carNum);
		for (int i = 0; i < Dispatch.carNum; i++) {
//			System.out.println("创建线程"+i);
			dispatch.gcurrentArea.add(0);
			dispatch.gcurrentTime.add(0);
			Car car = dispatch.new Car(i, initCarNum,countDownLatch); // 初始携带10辆单车
			dispatch.cars.add(car);
			car.start();
			countDownLatch.countDown();
		}
		//等待子线程结束
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
//			System.out.println("*****第"+car.id+"辆车结束运行，路径如下*****");
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
