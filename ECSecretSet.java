import java.util.ArrayList;
import java.util.Random;

class ECSecretSet
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
    private ECParam param;

    public MulECDSA()
    {
        this( Cnst.DEFAULT_MOD, Cnst.DEFAULT_SECRET_NUM );
    }
    
    public MulECDSA(long n, int secretNum)
    {
        // initialise instance variables
        this.n = n;
        d1 = new ECSecretSet( secretNum, n);
        d2 = new ECSecretSet( secretNum, n);
        param = new ECParam();
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
    // Parameter of Elliptic Curve y^2 mod n = x^3 + ax + b mod n
    // constraint: 4a^3 + 27b^2 mod n != 0
    private long a;
    private long b;
    private long n;

    public ECParam()
    {
        this( Cnst.DEFAULT_A, Cnst.DEFAULT_B, Cnst.DEFAULT_MOD );
    }

    public ECParam( long a, long b, long n )
    {
        this.a = a;
        this.b = b;
        this.n = n;
    }
    
    public long getA()
    {
        return a;
    }
    
    public long getB()
    {
        return b;
    }
    
    public long getN()
    {
        return n;
    }
    
    public boolean equals( ECParam p )
    {
        return (a == p.getA()) && (b == p.getB()) && (n == p.getN());
    }
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
        if( !param.equals( point.getParam() ))
            return null;
        
        long n = param.getN();
        long pY = point.getY();
        long pX = point.getX();
        
        long l = Cnst.mPos((y - pY) * Cnst.mInv(x - pX, n), n);
        long oX = Cnst.mPos(l*l - x - pX, n);
        long oY = Cnst.mPos( -y + l*(x - oX), n);
        
        return new ECPoint( oX, oY, param );
    }
    
    public ECPoint doubling()
    {
        long n = param.getN();
        long a = param.getA();

        long l = Cnst.mPos( (3*x*x + a) * Cnst.mInv(2*y, n), n);
        long oX = Cnst.mPos((l*l)-(2*x), n );
        long oY = Cnst.mPos( -y + l*(x - oX), n);
        
        return new ECPoint( oX, oY, param );
    }
    
    public ECPoint multiply( long m )
    {
        long n = param.getN();
        m %= n;
        long hob = Long.highestOneBit(m);
        ECPoint result, base;
        result = base = new ECPoint( x, y, param );
        
        // implement using basic double-and-add technique
        for( int i = 0; i < hob - 1; i++ ){
            result = result.doubling();
            m  = Long.rotateLeft( m, 1 );
            if( (m & hob) != 0 )
                result = result.add( base );
        }
        
        return result;
    }
    
    public ECPoint negative()
    {
        long newY = Cnst.mPos(-y, param.getN()) % param.getN();
        return new ECPoint( x, newY, param );
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
        return (param.equals( point.getParam())) &&
            (x == point.getX()) && (y == point.getY());
    }
}

final class Cnst
{
    // Default curve parameters
    public static final long DEFAULT_MOD = 2011;
    public static final long DEFAULT_A = 234;
    public static final long DEFAULT_B = 567;

    public static final int DEFAULT_SECRET_NUM = 5;
    
    // Modulo multiplication inverse using Extended Euclidean Algorithm (EEA)
    public static long mInv( long a, long n )
    {
        // Start EEA
        long alpha = 0;
        long beta = 1;

        long lastA = 1;
        long lastB = 0;

        while( n != 0 ) {
            long quotient = a/n;

            // (t, n) = ( n, t mod n )
            long tmp = a;
            a = n;
            n = tmp % n;

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
    
    // Modulo positive
    public static long mPos( long a, long n )
    {
        for( ; a < 0 ; a+=n );    
        return a;
    }
}
