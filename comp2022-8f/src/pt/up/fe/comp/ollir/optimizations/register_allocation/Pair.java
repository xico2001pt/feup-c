package pt.up.fe.comp.ollir.optimizations.register_allocation;

public class Pair<X, Y> { 
  public final X first; 
  public final Y second; 
  
  public Pair(X first, Y second) { 
    this.first = first; 
    this.second = second;
  }

  @Override
  public boolean equals(Object obj){
    if (this == obj) return true;
    if (!(obj instanceof Pair)) return false;
    Pair pair = (Pair) obj;
    return this.first.equals(pair.first) && this.second.equals(pair.second);
  }
} 
