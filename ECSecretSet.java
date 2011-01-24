import java.util.ArrayList;
import java.util.Random;
import java.math.BigInteger;

class ECSecretSet
{
    private ArrayList<BigInteger> d;

    public ECSecretSet(int num, BigInteger mod)
    {
        Random rd = new Random();
        d = new ArrayList<BigInteger>();

        int count = 0;
        while( count < num ) {
            // pick a random number
            BigInteger rand = new BigInteger( mod.bitLength(), rd );
            if( !d.contains( rand ) ) {
                d.add(rand);
                count++;
            }
        }
    }
    
    public String toString()
    {
        String out = "";
        for( int i = 0; i < d.size(); i++ ) {
            out += d.get(i).toString();
        }
        return out;
    }

}

class MulECDSA
{
    // instance variables - replace the example below with your own
    private BigInteger n;
    private ECSecretSet d1;
    private ECSecretSet d2;
    private ECParam param;

    public MulECDSA()
    {
        this( Cnst.ST192_N, Cnst.DEFAULT_SECRET_NUM );
    }
    
    public MulECDSA(BigInteger n, int secretNum)
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
    private BigInteger r;
    private BigInteger s;

    public MulECDSASig( BigInteger r, BigInteger s )
    {
        this.r = r;
        this.s = s;
    }

    public BigInteger getR()
    {
        return r;
    }
    
    public BigInteger getS()
    {
        return s;
    }
    
    public String toString()
    {
        return r + "," + s;
    }
}

class ECParam implements Cloneable
{
    // Parameter of Elliptic Curve y^2 mod n = x^3 + ax + b mod n
    // constraint: 4a^3 + 27b^2 mod n != 0
    private BigInteger a;
    private BigInteger b;
    private BigInteger n;
    private BigInteger gX;
    private BigInteger gY;

    public ECParam()
    {
        this( Cnst.ST192_A, Cnst.ST192_B, Cnst.ST192_N, Cnst.ST192_GX, Cnst.ST192_GY );
    }

    public ECParam( BigInteger a, BigInteger b, BigInteger n, BigInteger gX, BigInteger gY )
    {
        this.a = a;
        this.b = b;
        this.n = n;
        this.gX = gX;
        this.gY = gY;
    }
    
    public BigInteger getA()
    {
        return a;
    }
    
    public BigInteger getB()
    {
        return b;
    }
    
    public BigInteger getN()
    {
        return n;
    }
    
    public boolean equals( ECParam p )
    {
        return (a.equals(p.getA())) && (b.equals(p.getB())) && (n.equals(p.getN()));
    }

    protected Object clone() throws CloneNotSupportedException
    {
        ECParam clone = (ECParam)super.clone();
        return clone;
    }
}

class ECPoint implements Cloneable
{
    private BigInteger x;
    private BigInteger y;
    private ECParam param;

    public ECPoint( BigInteger x, BigInteger y, ECParam param )
    {
        this.x = x;
        this.y = y;
        this.param = param;
    }
    
    public ECPoint add( ECPoint point )
    {   
        BigInteger n = param.getN();
        BigInteger pY = point.getY();
        BigInteger pX = point.getX();
        
        //l = (y - pY)/(x - pX) mod n
        BigInteger l = ((y.subtract(pY)).multiply(x.subtract(pX).modInverse(n))).mod(n);
        //oX = (l^2 - x - pX) mod n
        BigInteger oX = ((l.pow(2)).subtract(x).subtract(pX)).mod(n);
        //oY = -y + l(x - oX) mod n;
        BigInteger oY = ((y.negate()).add(l.multiply(x.subtract(oX)))).mod(n);
        
        return new ECPoint( oX, oY, param );
    }
    
    public ECPoint doubling()
    {
        BigInteger n = param.getN();
        BigInteger a = param.getA();
        
        BigInteger three = BigInteger.valueOf(3);
        BigInteger two = BigInteger.valueOf(2);

        //l = (3x^2 + a)/(2y) mod n
        BigInteger l = (((x.pow(2).multiply(three)).add(a)).multiply(y.multiply(two))).mod(n);
        //oX = l^2 - 2x mod n
        BigInteger oX = (l.pow(2).subtract(x.multiply(two))).mod(n);
        //oY = -y + l(x - oX) mod n
        BigInteger oY = ((y.negate()).add(l.multiply(x.subtract(oX)))).mod(n);
        
        return new ECPoint( oX, oY, param );
    }
    
    public ECPoint multiply( BigInteger m )
    {
        BigInteger n = param.getN();
        m = m.mod(n);
        int hob = m.bitLength();
        ECPoint result, base;
        result = base = new ECPoint( x, y, param );
        
        // implement using basic double-and-add technique
        for( int i = 0; i < hob - 1; i++ ){
            result = result.doubling();
            m  = m.shiftLeft(1);
            if( m.testBit(hob) ) {
                result = result.add( base );
            }
        }
        
        return result;
    }
    
    public ECPoint negative()
    {
        BigInteger newY = (y.negate()).mod(param.getN());
        return new ECPoint( x, newY, param );
    }
    
    public BigInteger getX()
    {
        return x;
    }
    
    public BigInteger getY()
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
            (x.equals(point.getX())) && (y.equals(point.getY()));
    }

    protected Object clone() throws CloneNotSupportedException
    {
        ECPoint clone = (ECPoint) super.clone();
        clone.param = (ECParam) param.clone();
        return clone;
    }
}

final class Cnst
{
    // NIST standard curve P-192 parameters
    public static final BigInteger ST192_N = new BigInteger("6277101735386680763835789423207666416083908700390324961279");
    public static final BigInteger ST192_A = new BigInteger("-3");
    public static final BigInteger ST192_B = new BigInteger("64210519e59c80e70fa7e9ab72243049feb8deecc146b9b1",16);
    public static final BigInteger ST192_GX = new BigInteger("188da80eb03090f67cbf20eb43a18800f4ff0afd82ff1012",16);
    public static final BigInteger ST192_GY = new BigInteger("07192b95ffc8da78631011ed6b24cdd573f977a11e794811",16);

    public static final int DEFAULT_SECRET_NUM = 5;
    
    // Modulo multiplication inverse using Extended Euclidean Algorithm (XEA)
    //**** UNUSED b/c BigInteger has built-in modInverse() method *****
    public static long mInv( long a, long n )
    {
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
    // **** UNUSED b/c BigInteger's mod() always return non-negative ****
    public static BigInteger mPos( BigInteger a, BigInteger n )
    {
        while( a.compareTo(BigInteger.ZERO) < 0 ) {
            a = a.add(n);
        }
        return a;
    }
}
