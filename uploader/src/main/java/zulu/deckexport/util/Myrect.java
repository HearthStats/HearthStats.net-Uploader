/**
 * @author shyos
 */
package zulu.deckexport.util;

public class Myrect {
	private int x;
	private int y;
	private int w;
	private int h;
	
	public Myrect(int x, int y, int w, int h) {
		super();
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getW() {
		return w;
	}
	public void setW(int w) {
		this.w = w;
	}
	public int getH() {
		return h;
	}
	public void setH(int h) {
		this.h = h;
	}
	public String toString(){
		return x +"," +y  +"," + w +"," + h; 
	}
}
