import java.io.File;
import lejos.hardware.motor.*;
import lejos.hardware.lcd.*;
import lejos.hardware.Sound.*;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.port.*;
import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.ev3.EV3;
import lejos.hardware.Keys;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.hardware.sensor.*;
import lejos.utility.Delay;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.Device.*;
import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.robotics.RegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import java.util.Random;

class TicTacToe_v3 {
	public static void main(String[] args) {
		 UnregulatedMotor motorA_power = new UnregulatedMotor(MotorPort.A);
		 Brick brick = BrickFinder.getDefault();
		 Port s1 = brick.getPort("S1");
         Port s2 = brick.getPort("S2");
         Port s3 = brick.getPort("S3");
         Port s4 = brick.getPort("S4");

         EV3ColorSensor colorSensor_1 = new EV3ColorSensor(s1);
	     SampleProvider colorReader_1 = colorSensor_1.getColorIDMode();
		 float[] colorSample_1 = new float[colorReader_1.sampleSize()];

		 EV3ColorSensor colorSensor_2 = new EV3ColorSensor(s2);
         SampleProvider colorReader_2 = colorSensor_2.getColorIDMode();
         float[] colorSample_2 = new float[colorReader_2.sampleSize()];

         EV3ColorSensor colorSensor_3 = new EV3ColorSensor(s3);
         SampleProvider colorReader_3 = colorSensor_3.getColorIDMode();
         float[] colorSample_3 = new float[colorReader_3.sampleSize()];

         SampleProvider pressSensor = new EV3TouchSensor(s4);
		 float[] pressSample = new float[pressSensor.sampleSize()];

		 //Sound sound = BrickFinder.getDefault().getSound();
		 GameEngine engine = new GameEngine();
		 boolean play = true;
		 boolean start = true;
		 while (play) {
			 if (start) {
				  System.out.print("If you want to start, place a piece on the board.\n");
				  Sound.playSample(new File(engine.getMoveSound(3, 4)), Sound.VOL_MAX);
			 }
			 while(start) {
				 pressSensor.fetchSample(pressSample, 0);
				 if (pressSample[0] > 0) {
				 	start = false;
				 }
			 }
			 for (int i = 0; i < 3; i++) {
				motorA_power.setPower(20);
				Delay.msDelay(600);
				motorA_power.setPower(0);

				colorReader_1.fetchSample(colorSample_1, 0);
				colorReader_2.fetchSample(colorSample_2, 0);
				colorReader_3.fetchSample(colorSample_3, 0);

				double value1 = colorSample_1[0];
				double value2 = colorSample_2[0];
				double value3 = colorSample_3[0];

				System.out.println(String.format("%.2f", value1));
				System.out.println(String.format("%.2f", value2));
				System.out.println(String.format("%.2f", value3));

				Delay.msDelay(2000);

				engine.fillBoard(value1, value2, value3, i);
			}
			motorA_power.setPower(-20);
			Delay.msDelay(1800);
			motorA_power.setPower(0);

			engine.count(0, 1);
			engine.count(1, 2);

			int[] moveRobot = engine.findMove(0);
			int[] movePlayer = engine.findMove(1);
			play = engine.isDone();

			int recentX;
			int recentY;
			if (play) {
				if (moveRobot[2] > movePlayer[2]) {
					 System.out.println("Make move: (" +(moveRobot[0]+1)+ ", " +(moveRobot[1]+1)+ ")\n");
					 engine.setMove(moveRobot[0], moveRobot[1]);
					 recentX = moveRobot[0];
					 recentY = moveRobot[1];

				     Sound.playSample(new File(engine.getMoveSound(recentX, recentY)), Sound.VOL_MAX);
					 Sound.playSample(new File(engine.getMoveSound(3, 0)), Sound.VOL_MAX);
				} else {
					 System.out.println("Make move: (" +(movePlayer[0]+1)+ ", " +(movePlayer[1]+1)+ ")\n");
					 engine.setMove(movePlayer[0], movePlayer[1]);
					 recentX = movePlayer[0];
					 recentY = movePlayer[1];

					 Sound.playSample(new File(engine.getMoveSound(recentX, recentY)), Sound.VOL_MAX);
					 Sound.playSample(new File(engine.getMoveSound(3, 0)), Sound.VOL_MAX);
				}
			}
			else {
				System.out.println("It's a draw!");
				Sound.playSample(new File(engine.getMoveSound(3, 2)), Sound.VOL_MAX);
				Delay.msDelay(5000);
				break;
			}
			System.out.println(engine.writeBoard(recentX, recentY));

			engine.count(0, 1);
			engine.count(1, 2);

			moveRobot = engine.findMove(0);
			movePlayer = engine.findMove(1);
			play = engine.isDone();

			if (!play) {
				if (moveRobot[2] == 11) {
					System.out.println("It's a draw!");
					Sound.playSample(new File(engine.getMoveSound(3, 2)), Sound.VOL_MAX);
				}
				else if (moveRobot[2] == 10) {
					System.out.println("You lost! Better luck next time.");
					Sound.playSample(new File(engine.getMoveSound(3, 1)), Sound.VOL_MAX);
				}
			}

			boolean nextTurn = true;
			while (nextTurn) {
				pressSensor.fetchSample(pressSample, 0);
				if (pressSample[0] > 0) {
					nextTurn = false;
				}
			}
		}
		Sound.playSample(new File(engine.getMoveSound(3, 5)), Sound.VOL_MAX);
	}
}


class GameEngine {
	//Lager en todimensjonal tabell av brettet, hvor verdien 0 svarer til tom rute, verdien 1 for maskinen og verdien 2 for spilleren.
	private int[][] board = new int[3][3];
	//Teller opp hvor mange brikker av samme type det er på hver rekke på brettet.
	private int counter[][] = new int[2][8];
	//Tabell som angir hvilke punkter som inngår i hver rekke på brettet.
	private final int[][][] findMove =   {{{0,0}, {0,1}, {0,2}},
										  {{1,0}, {1,1}, {1,2}},
										  {{2,0}, {2,1}, {2,2}},
										  {{0,0}, {1,0}, {2,0}},
										  {{0,1}, {1,1}, {2,1}},
										  {{0,2}, {1,2}, {2,2}},
										  {{0,2}, {1,1}, {2,0}},
										  {{0,0}, {1,1}, {2,2}}};
	//Tabell som inneholder alle replikkene til roboten.
	private final String[][] moveSound =  {{"1-1_trekk.wav", "1-2_trekk.wav", "1-3_trekk.wav"},
										   {"2-1_trekk.wav", "2-2_trekk.wav", "2-3_trekk.wav"},
										   {"3-1_trekk.wav", "3-2_trekk.wav", "3-3_trekk.wav"},
										   {"Spiller_Gjør_Trekk.wav", "Spiller_Tap_LOL.wav", "Spiller_Uavgjort.wav",
										    "Spiller_Vinn_Juks.wav", "Start_Spillet.wav", "Team_10_Hilsen.wav"}};
	//Leser av verdiene fra sensoren og fyller opp board-tabellen.
	public void fillBoard(double value1, double value2, double value3, int row) {
			for (int i = 0; i < 3; i++) {
				double value = 0;
				if (i == 0) {
					value = value1;
				}
				else if (i == 1) {
					value = value2;
				}
				else if (i == 2) {
					value = value3;
				}
				if (value == 0 || value == 13) {
					board[row][i] = 1;
				}
				else if (value == 2) {
					board[row][i] = 2;
				}
				else {
					board[row][i] = 0;
			    }
		}
	}
	//Fyller opp counter-tabellen.
	public void count(int player, int color) {
			for (int i = 0; i < 8; i++) {
				counter[player][i] = 0;
			}
			if (board[0][0] == color) {
				counter[player][0]++;
				counter[player][3]++;
				counter[player][7]++;
			}
			if (board[0][1] == color) {
				counter[player][0]++;
				counter[player][4]++;
			}
			if (board[0][2] == color) {
				counter[player][0]++;
				counter[player][5]++;
				counter[player][6]++;
			}
			if (board[1][0] == color) {
				counter[player][3]++;
				counter[player][1]++;
			}
			if (board[1][1] == color) {
				counter[player][1]++;
				counter[player][4]++;
				counter[player][6]++;
				counter[player][7]++;
			}
			if (board[1][2] == color) {
				counter[player][1]++;
				counter[player][5]++;
			}
			if (board[2][0] == color) {
				counter[player][2]++;
				counter[player][3]++;
				counter[player][6]++;
			}
			if (board[2][1] == color) {
				counter[player][2]++;
				counter[player][4]++;
			}
			if (board[2][2] == color) {
				counter[player][2]++;
				counter[player][5]++;
				counter[player][7]++;
			}
	}
	//Bruker counter- og board-tabllene til å finne det beste trekket.
	public int[] findMove(int type) {
		//Lager en tabell hvor de to første verdiene innholder koordinatene til trekket, og en tredje verdi som angir hvor bra trekket er.
		int[] move = new int[3];
		for (int i = 0; i < counter[type].length; i++) {
			//Hvis en spiller har 3 på rad er spillet over.
			if (counter[type][i] == 3) {
				move[2] = 10;
				return move;
			}
			//Sjekker om det er 2 på rad noen plasser, og den tredje plassen i rekken er åpen.
			else if (counter[type][i] == 2) {
				for (int j = 0; j < 3; j++) {
					int x = findMove[i][j][0];
					int y = findMove[i][j][1];

					if (board[x][y] == 0) {
						move[0] = x;
						move[1] = y;
						if (type == 0) {
							move[2] = 9;
						} else if (type == 1) {
							move[2] = 8;
						}
						return move;
					}
				}
			}
		}
		//Hvis det ikke er mulig å få 3 på rad sjekkes det om midten er ledig.
		if (board[1][1] == 0) {
			move[0] = 1;
			move[1] = 1;
			move[2] = 7;
			return move;
		}
		//Hvis midten ikke er ledig sjekkes det om det er 1 på rad og de to andre plassene på rekken er åpen.
		for (int i = 0; i < counter[type].length; i++) {
			if (counter[type][i] == 1) {
				int zero = 0;
				for (int j = 0; j < 3; j++) {
					int x = findMove[i][j][0];
					int y = findMove[i][j][1];
					if (board[x][y] == 0) {
						zero++;
					}
					if (zero == 2) {
						move[0] = x;
						move[1] = y;
						if (board[1][1] == 2) {
							Random random = new Random();
							int xCorner = 0;
							int yCorner = 0;
							if ((random.nextInt(2) + 1) == 1) {
								xCorner = 0;
							} else {
								xCorner = 2;
							}
							if ((random.nextInt(2) + 1) == 1) {
								yCorner = 0;
							} else {
								yCorner = 2;
							}
							move[0] = xCorner;
							move[1] = yCorner;
							move[2] = 6;
							return move;
						}
						if (type == 0) {
							move[2] = 5;
						} else if (type == 1) {
							move[2] = 4;
						}
						return move;
					}
				}
	 		}
	    }
	    //Hvis ingen av mulighetene ovenfor er oppfylt velges bare en åpen plass på brettet.
	    for(int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (board[i][j] == 0) {
					move[0] = i;
					move[1] = j;
					move[2] = 1;
					return move;
				}
		    }
		}
		//Hvis koden kommer hit er brettet helt fullt.

		move[2] = 11;
		return move;
	}
	public String writeBoard(int recentX, int recentY) {
		String output = "";
		for (int i = 0; i < 3; i++) {
			 for (int j = 0; j < 3; j++) {
				 if (board[i][j] == 0) {
					 output += "--  ";
				 }
				 else if (board[i][j] == 1) {
					 if (i == recentX && j == recentY) {
						 output += "(X) ";
					 }
					 else {
						 output += "X  ";
					 }
				 }
				 else if (board[i][j] == 2) {
					 output += "O  ";
				 }
			 }
			 output += "\n\n";
		}
		return output;
	}
	//Bruker en metode til å plassere roboten sitt trekk i board-tabellen.
	public void setMove(int x, int y) {
		board[x][y] = 1;
	}
	public String getMoveSound(int x, int y) {
		return moveSound[x][y];
	}
	//Sjekker om spillet er ferdig, enten ved at en spiller har vunnet eller at det er uavgjort.
	public boolean isDone() {
		if (findMove(0)[2] == 11 || findMove(0)[2] == 10) {
			return false;
		}
		if (findMove(1)[2] == 11 || findMove(1)[2] == 10) {
			return false;
		}
		return true;
	}

}