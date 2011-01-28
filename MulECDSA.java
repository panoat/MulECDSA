import java.util.ArrayList;
import java.util.Random;
import java.math.BigInteger;
import java.security.*;

class MulECDSA
{
    private Signer sgn;
    private Verifier vfr;

    public MulECDSA()
    {
        ECPoint g = new ECPoint( Cnst.ST192_GX, Cnst.ST192_GY, new ECParam() );
        ECSecretSet d1 = new ECSecretSet( Cnst.DEFAULT_SECRET_NUM, g );
        ECSecretSet d2 = new ECSecretSet( Cnst.DEFAULT_SECRET_NUM, g );

        sgn = new Signer( d1, d2, g );
        vfr = new Verifier( d1.genPublicSet(), d2.genPublicSet(), g );
    }

    public MulECDSA( Signer sgn, Verifier vfr )
    {
        this.sgn = sgn;
        this.vfr = vfr;
    }

    public void runTest( String msg ) throws NoSuchAlgorithmException
    {
        // Find message's MD5 hash in BigInteger format
        byte[] msgByte = msg.getBytes();
        MessageDigest md = MessageDigest.getInstance("MD5");
        BigInteger msgHash = new BigInteger( md.digest(msgByte) );

        System.out.println("Signing message = " + msg );
        System.out.println("Message MD5     = " + msgHash.toString(16) );

        MulECDSASig sig = sgn.sign( msgHash );
        System.out.println("MulECDSA Signature\n" + sig );

        System.out.println("The signature is " + (vfr.verify(msgHash,sig)?"":"NOT") + " verified." );
    }
}

class ECSecretSet
{
    private ArrayList<BigInteger> d;
    private ECPoint g;

    public ECSecretSet(int num, ECPoint g)
    {
        Random rd = new Random();
        d = new ArrayList<BigInteger>();

        int count = 0;
        while( count < num ) {
            // pick a random number
            BigInteger rand = Cnst.rand(g.getParam().getN());

            if( !d.contains( rand ) ) {
                d.add(rand);
                count++;
            }
        }
        this.g = g;
    }

    public BigInteger get( int i )
    {
        return d.get(i);    
    }

    public int size()
    {
        return d.size();
    }

    public ECPublicSet genPublicSet()
    {
        ArrayList<ECPoint> pub = new ArrayList<ECPoint>();

        for( BigInteger i : d ) {
            pub.add( g.multiply(i) );  
        }

        return new ECPublicSet(pub, g);
    }

    public String toString()
    {
        String out = "";
        for( int i = 0; i < d.size(); i++ ) {
            out += d.get(i).toString() + "\n";
        }
        return out;
    }

    /*
     * Perform coefficient multiplication
     * return Sum( c_i * d_i ) where 0 <= i <= set size
     * and c_i = integer extracted from bit 0*i to  ceiling(n bit size / set size)*i (exclusive)
     */
    public BigInteger coefMultiply( BigInteger c )
    {
        int nBitLen = g.getParam().getN().bitLength();
        int bsize = Cnst.splitBitLength( nBitLen, d.size() );
        ArrayList<BigInteger> cArry = Cnst.split( c, bsize );       // split 'c' according to 'bsize'

        BigInteger out = BigInteger.ZERO;
        for( int i = 0; i < cArry.size(); i++ ) {
            out = out.add( d.get(i).multiply( cArry.get(i) ) );
        }

        return out;
    }
}

class ECPublicSet
{
    private ArrayList<ECPoint> q;
    private ECPoint g;

    public ECPublicSet( ArrayList<ECPoint> q, ECPoint g )
    {
        this.q = q;
        this.g = g;
    }

    /*
     * Perform coefficient multiplication
     * return Sum( c_i * q_i ) where 0 <= i <= set size
     * and c_i = integer extracted from bit 0*i to  ceiling(n bit size / set size)*i (exclusive)
     */
    public ECPoint coefMultiply( BigInteger c )
    {
        int nBitLen = g.getParam().getN().bitLength();
        int bsize = Cnst.splitBitLength( nBitLen, q.size() );
        ArrayList<BigInteger> cArry = Cnst.split( c, bsize );       // split 'c' according to 'bsize'

        ECPoint out = q.get(0).multiply( cArry.get(0));            // start at first point in the set
        for( int i = 1; i < cArry.size(); i++ ) {
            out = out.add( q.get(i).multiply( cArry.get(i)) );
        }
        return out;
    }
}

class Signer
{
    private ECSecretSet d1;
    private ECSecretSet d2;
    private ECPoint g;          // EC group generator

    public Signer()
    {
        this( Cnst.DEFAULT_SECRET_NUM, 
            new ECPoint( Cnst.ST192_GX, Cnst.ST192_GY, new ECParam() ) );
    }

    public Signer( ECSecretSet d1, ECSecretSet d2, ECPoint g )
    {
        this.d1 = d1;
        this.d2 = d2;
        this.g = g;
    }

    public Signer(int secretNum, ECPoint g)
    {
        this( new ECSecretSet( secretNum, g ),new ECSecretSet( secretNum, g ), g);
    }

    public MulECDSASig sign( BigInteger msgHash )
    {
        BigInteger n = g.getParam().getN();
        BigInteger k = Cnst.rand(n);

        BigInteger r = g.multiply(k).getX();    // x-coor of random point kG
        BigInteger steps = d2.coefMultiply(r).add(d1.coefMultiply(msgHash));
        BigInteger s = steps.modInverse(n).multiply(k).mod(n);

        return new MulECDSASig( r, s );
    }
}

class Verifier
{
    private ECPublicSet q1;
    private ECPublicSet q2;
    private ECPoint g;

    public Verifier(ECPublicSet q1, ECPublicSet q2, ECPoint g )
    {
        this.q1 = q1;
        this.q2 = q2;
        this.g = g;
    }

    public boolean verify( BigInteger msgHash, MulECDSASig sig )
    {
        BigInteger r = sig.getR();
        BigInteger s = sig.getS();
        BigInteger n = g.getParam().getN();

        BigInteger u1 = r.multiply(s).mod(n);
        BigInteger u2 = msgHash.multiply(s).mod(n);

        BigInteger rP = (q2.coefMultiply(u1).add( q1.coefMultiply(u2) )).getX();

        System.out.println("Verifying signature");
        System.out.println("r  = " + r.toString(16) );
        System.out.println("r\' = " + rP.toString(16) );

        return rP.equals(r);
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
        return "r = " + r.toString(16) + "\ns = " + s.toString(16) + "\n";
    }
}

class ECParam implements Cloneable
{
    // Parameter of Elliptic Curve y^2 mod n = x^3 + ax + b mod n
    // constraint: 4a^3 + 27b^2 mod n != 0
    private BigInteger a;
    private BigInteger b;
    private BigInteger n;

    public ECParam()
    {
        this( Cnst.ST192_A, Cnst.ST192_B, Cnst.ST192_N);
    }

    public ECParam( BigInteger a, BigInteger b, BigInteger n )
    {
        this.a = a;
        this.b = b;
        this.n = n;
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

    public String toString()
    {
        return "x = " + x.toString(16) + "\ny = " + y.toString(16) + "\n";
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

    // Generate random BigInteger that fall in the range [0, max)
    public static BigInteger rand( BigInteger max )
    {
        Random rd = new Random();
        BigInteger rand;    
        do {
            rand = new BigInteger(max.bitLength(), rd);
        } while (rand.compareTo(max) >= 0);

        return rand;
    }

    // split a target integer into smaller integers with bit length = 'size'
    public static ArrayList<BigInteger> split( BigInteger target, int size )
    {
        BigInteger exp = (new BigInteger("2")).pow( size );         // use for extraction
        BigInteger tmp = new BigInteger( target.toString());        // copy to protect the original value
        ArrayList<BigInteger> out = new ArrayList<BigInteger>();

        // extraction loop
        while( tmp.compareTo(BigInteger.ZERO) > 0 ) {
            out.add( tmp.mod(exp) );
            tmp = tmp.divide(exp);
        }

        return out;
    }

    public static int splitBitLength( int targetLen, int num )
    {
        return (int)Math.ceil( targetLen / (double)num );
    }

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
