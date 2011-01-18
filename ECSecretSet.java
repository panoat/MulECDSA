import java.util.ArrayList;
import java.util.Random;

public class ECSecretSet
{
    private long d[];

    public ECSecretSet(int num, long mod)
    {
        ArrayList<Long> aval = new ArrayList<Long>();
        Random rd = new Random();
        d = new long[num];

        for( long i = 0; i < mod; i++ ) {
            aval.add( i );
        }

        for( int i = 0; i < num; i++ ) {
            long pick = rd.nextLong() % aval.size();
            d[i] = aval.get((int)pick);
            aval.remove( (int)pick );
        }
    }
    
    public String toString()
    {
        String out = "";
        for( int i = 0; i < d.length; i++ ) {
            out += d[i] + "\n";
        }
        return out;
    }

}

class MulECDSA
{
    // instance variables - replace the example below with your own
    private long n;
    private ECSecretSet d1;
    private ECSecretSet d2;

    public MulECDSA()
    {
        this( 2011, 5 );
    }
    
    public MulECDSA(long n, int secretNum)
    {
        // initialise instance variables
        this.n = n;
        d1 = new ECSecretSet( secretNum, n);
        d2 = new ECSecretSet( secretNum, n);
    }

    private long calcEEA( long a, long b )
    {
        // Start EEA
        long alpha = 0;
        long beta = 1;

        long lastA = 1;
        long lastB = 0;

        while( b != 0 ) {
            long quotient = a/b;

            // (t, n) = ( n, t mod n )
            long tmp = a;
            a = b;
            b = tmp % b;

            // (alpha, lastA) = ( lastA - quotient*alpha, alpha )
            tmp = lastA;
            lastA = alpha;
            alpha = tmp - ( quotient * alpha );

            // (beta, lastB) = ( lastB - quotient*beta, beta )
            tmp = lastB;
            lastB = beta;
            beta = tmp - ( quotient * alpha );
        }

        return lastA;
    }
    
}

class MulECDSASig
{
    private long r;
    private long s;

    public MulECDSASig( long r, long s )
    {
        this.r = r;
        this.s = s;
    }

    public long getR()
    {
        return r;
    }
    
    public long getS()
    {
        return s;
    }
    
    public String toString()
    {
        return r + "," + s;
    }
}

class ECParam
{




}

class ECPoint
{
    private long x;
    private long y;
    private ECParam param;

    public ECPoint( long x, long y, ECParam param )
    {
        this.x = x;
        this.y = y;
        this.param = param;
    }
    
    public ECPoint add( ECPoint point )
    {
        // check that EC points are on the same curve
        
        
    }
    
    public ECPoint square()
    {
        
    }
    
    public ECPoint multiply( long m )
    {
        
        
    }
    
    public long getX()
    {
        return x;
    }
    
    public long getY()
    {
        return y;
    }
    
    public ECParam getParam()
    {
        return param;
    }
    
    public boolean equals( ECPoint point )
    {
        if( param.equals( point.getParam()) &&
            x == point.getX() && y == point.getY() )
            return true;
            
        return false;
    }
}
