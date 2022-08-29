package pt.up.fe.comp.jasmin;

import java.lang.Math;

public class StackLimits {
    private int stackSize;
    private int maxStackSize;

    public StackLimits() {
        this.reset();
    }

    public int getMaxStackSize() {
        return this.maxStackSize;
    }

    public boolean isEmpty() {
        return this.stackSize == 0;
    }

    public void update(int delta) {
        this.stackSize += delta;
        this.maxStackSize = Math.max(this.maxStackSize, this.stackSize);
    }

    public void reset() {
        this.stackSize = 0;
        this.maxStackSize = 0;
    }

    public String toString() {
        return "stackSize=" + this.stackSize + "\nmaxStackSize=" + this.maxStackSize;
    }
}