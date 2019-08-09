package com.github.egor18.jdataflow.testcases;

public class TestConditionalOperator
{
    void testConditionalOperator1()
    {
        int x = 3;
        int y = (x == 3) ? 5 : 2; //@ALWAYS_TRUE
        if (y == 5) {} //@ALWAYS_TRUE
    }

    void testConditionalOperator2()
    {
        int x = 3;
        int z = (x == 3) ? ++x : --x; //@ALWAYS_TRUE
        if (z == 4) {} //@ALWAYS_TRUE
        if (x == 4) {} //@ALWAYS_TRUE
    }

    void testConditionalOperator3(int x)
    {
        boolean z = x > 3 ? x == 1 : x == 0; //@ALWAYS_FALSE
    }

    void testConditionalOperator4(boolean cond)
    {
        Object x = cond ? new Integer(42) : new Double(14.0);
        if (x == null) {} //@ALWAYS_FALSE
        if ((Integer) x == 42) {}
    }

    void testConditionalOperator5(boolean cond)
    {
        Object y = cond ? new Integer(42) : new Integer(14);
        if (y == null) {} //@ALWAYS_FALSE
        if ((Integer) y == 42) {}
    }

    void testConditionalOperator6(boolean cond)
    {
        class P {}
        class D {}
        P p = new P();
        D d = new D();
        Object pd = cond ? p : d;
        if (pd == null) {} //@ALWAYS_FALSE
    }

    void testConditionalOperator7(Object obj)
    {
        if((obj instanceof Boolean) ? (boolean)obj : false) {}
    }

    void testConditionalOperatorTypes1(boolean cond, Integer i, Object obj)
    {
        Object k1 = cond ? 10 : 10.3f;

        Object k2 = cond ? 10 : 20;

        int k3 = cond ? 10 : 20;

        Long k4 = cond ? 10 : 20L;
        if (cond)
        {
            if (k4 == 10) {} //@ALWAYS_TRUE
            if (k4 == 20L) {} //@ALWAYS_FALSE
        }
        else
        {
            if (k4 == 10) {} //@ALWAYS_FALSE
            if (k4 == 20L) {} //@ALWAYS_TRUE
        }

        long k5 = cond ? 10 : 20L;

        long k6 = cond ? 10 : 20;

        Long k7 = cond ? (long) i : 20;

        Integer k8 = cond ? new Integer(10) : 20;
        if (cond)
        {
            if (k8 == 10) {}
            if (k8 == 20) {}
        }
        else
        {
            if (k8 == 10) {} //@ALWAYS_FALSE
            if (k8 == 20) {} //@ALWAYS_TRUE
        }

        Long k9 = cond ? (int)new Integer(10) : 20L;
        if (cond)
        {
            if (k9 == 10) {} //@ALWAYS_TRUE
            if (k9 == 20L) {} //@ALWAYS_FALSE
        }
        else
        {
            if (k9 == 10) {} //@ALWAYS_FALSE
            if (k9 == 20L) {} //@ALWAYS_TRUE
        }

        boolean k10 = (obj instanceof Boolean) ? (boolean)obj : true;
    }

    void testConditionalOperatorTypes2(boolean cond)
    {
        float z1 = cond ? 10.0f : 20.0f;
        double z2 = cond ? 10.0f : 20.0;
        Float z3 = cond ? 10.0f : 20.0f;
        Double z4 = cond ? 10.0f : 20.0;
        Object z5 = cond ? new Float(10.0f) : new Double(20.0);
        Object z6 = cond ? 10.0f : new Double(20.0);

        Object z7 = cond ? 10 : new Double(20.0);
        float z8 = cond ? 10 : 20.0f;

        Object x = cond ? new Integer(42) : new Double(14.0);
        if (x == null) {} //@ALWAYS_FALSE
        if ((Integer) x == 42) {}
    }

    void testConditionalOperatorBooleanType1(boolean cond)
    {
        boolean b1 = cond ? true : false;
        boolean b2 = cond ? new Boolean(true) : false;
        boolean b3 = cond ? true : new Boolean(false);
        boolean b4 = cond ? new Boolean(true) : new Boolean(false);
        if (b1) {}
        if (b2) {}
        if (b3) {}
        if (b4) {}
    }

    void testConditionalOperatorTypes3(boolean cond)
    {
        Integer a = cond ? new Integer(10) : 20;
        Object b = cond ? new Integer(10) : 20;
        int c = cond ? new Integer(10) : 20;
        long d = cond ? new Integer(10) : 20;
    }

    void testConditionalOperatorTypes4(boolean cond)
    {
        Object k1 = cond ? new Integer(10) : new Integer(20);
        if (k1 == null) {} //@ALWAYS_FALSE

        Object k2 = cond ? 10 : 20L;
        if (k2 == null) {} //@ALWAYS_FALSE

        Object k3 = cond ? new Integer(10) : new Long(20L);
        if (k3 == null) {} //@ALWAYS_FALSE

        Object k4 = cond ? new Integer(10) : 20L;
        if (k4 == null) {} //@ALWAYS_FALSE

        Object k5 = cond ? new Integer(10) : false;
        if (k5 == null) {} //@ALWAYS_FALSE

        Object k6 = cond ? 10 : false;
        if (k6 == null) {} //@ALWAYS_FALSE

        Object k7 = cond ? 10 : new Integer(20);
        if (k7 == null) {} //@ALWAYS_FALSE

        Object k8 = cond ? null : new Integer(10);
        if (k8 == null) {} //ok

        Object k9 = cond ? new Integer(42) : new Integer(14);
        if (k9 == null) {} //@ALWAYS_FALSE
        if ((Integer)k9 == 42) {}

        Object k10 = cond ? 10 : 20;
        if (k10 == null) {} //@ALWAYS_FALSE

        Object k11 = cond ? new Integer(10) : new Integer(20);
        if (k11 == null) {} //@ALWAYS_FALSE

        Integer i1 = new Integer(10);
        Integer i2 = new Integer(20);
        Object k12 = cond ? i1 : i2;
        if (k12 == null) {} //@ALWAYS_FALSE

        Object k13 = cond ? new Integer(10) : 20;
        if (k13 == null) {} //@ALWAYS_FALSE

        short s = 20;
        Object k14 = cond ? new Integer(10) : new Short(s);
        if (k14 == null) {} //@ALWAYS_FALSE

        Object k15 = cond ? new Object() : new Object();
        if (k15 == null) {} //@ALWAYS_FALSE

        Integer k16 = cond ? new Integer(10) : new Integer(20);
        if (k16 == 40) {} //@ALWAYS_FALSE
    }

    void testAllTypeCombinations(boolean cond, byte b, short s, char c, int i, long l, float f, double d, boolean bo,
                                 Byte wb, Short ws, Character wc, Integer wi, Long wl, Float wf, Double wd, Boolean wbo, Object o)
    {
        Object k1 = cond ? b : b;
        Object k2 = cond ? b : s;
        Object k3 = cond ? b : c;
        Object k4 = cond ? b : i;
        Object k5 = cond ? b : l;
        Object k6 = cond ? b : f;
        Object k7 = cond ? b : d;
        Object k8 = cond ? b : bo;
        Object k9 = cond ? b : wb;
        Object k10 = cond ? b : ws;
        Object k11 = cond ? b : wc;
        Object k12 = cond ? b : wi;
        Object k13 = cond ? b : wl;
        Object k14 = cond ? b : wf;
        Object k15 = cond ? b : wd;
        Object k16 = cond ? b : wbo;
        Object k17 = cond ? b : o;
        Object k18 = cond ? b : null;
        Object k19 = cond ? s : b;
        Object k20 = cond ? s : s;
        Object k21 = cond ? s : c;
        Object k22 = cond ? s : i;
        Object k23 = cond ? s : l;
        Object k24 = cond ? s : f;
        Object k25 = cond ? s : d;
        Object k26 = cond ? s : bo;
        Object k27 = cond ? s : wb;
        Object k28 = cond ? s : ws;
        Object k29 = cond ? s : wc;
        Object k30 = cond ? s : wi;
        Object k31 = cond ? s : wl;
        Object k32 = cond ? s : wf;
        Object k33 = cond ? s : wd;
        Object k34 = cond ? s : wbo;
        Object k35 = cond ? s : o;
        Object k36 = cond ? s : null;
        Object k37 = cond ? c : b;
        Object k38 = cond ? c : s;
        Object k39 = cond ? c : c;
        Object k40 = cond ? c : i;
        Object k41 = cond ? c : l;
        Object k42 = cond ? c : f;
        Object k43 = cond ? c : d;
        Object k44 = cond ? c : bo;
        Object k45 = cond ? c : wb;
        Object k46 = cond ? c : ws;
        Object k47 = cond ? c : wc;
        Object k48 = cond ? c : wi;
        Object k49 = cond ? c : wl;
        Object k50 = cond ? c : wf;
        Object k51 = cond ? c : wd;
        Object k52 = cond ? c : wbo;
        Object k53 = cond ? c : o;
        Object k54 = cond ? c : null;
        Object k55 = cond ? i : b;
        Object k56 = cond ? i : s;
        Object k57 = cond ? i : c;
        Object k58 = cond ? i : i;
        Object k59 = cond ? i : l;
        Object k60 = cond ? i : f;
        Object k61 = cond ? i : d;
        Object k62 = cond ? i : bo;
        Object k63 = cond ? i : wb;
        Object k64 = cond ? i : ws;
        Object k65 = cond ? i : wc;
        Object k66 = cond ? i : wi;
        Object k67 = cond ? i : wl;
        Object k68 = cond ? i : wf;
        Object k69 = cond ? i : wd;
        Object k70 = cond ? i : wbo;
        Object k71 = cond ? i : o;
        Object k72 = cond ? i : null;
        Object k73 = cond ? l : b;
        Object k74 = cond ? l : s;
        Object k75 = cond ? l : c;
        Object k76 = cond ? l : i;
        Object k77 = cond ? l : l;
        Object k78 = cond ? l : f;
        Object k79 = cond ? l : d;
        Object k80 = cond ? l : bo;
        Object k81 = cond ? l : wb;
        Object k82 = cond ? l : ws;
        Object k83 = cond ? l : wc;
        Object k84 = cond ? l : wi;
        Object k85 = cond ? l : wl;
        Object k86 = cond ? l : wf;
        Object k87 = cond ? l : wd;
        Object k88 = cond ? l : wbo;
        Object k89 = cond ? l : o;
        Object k90 = cond ? l : null;
        Object k91 = cond ? f : b;
        Object k92 = cond ? f : s;
        Object k93 = cond ? f : c;
        Object k94 = cond ? f : i;
        Object k95 = cond ? f : l;
        Object k96 = cond ? f : f;
        Object k97 = cond ? f : d;
        Object k98 = cond ? f : bo;
        Object k99 = cond ? f : wb;
        Object k100 = cond ? f : ws;
        Object k101 = cond ? f : wc;
        Object k102 = cond ? f : wi;
        Object k103 = cond ? f : wl;
        Object k104 = cond ? f : wf;
        Object k105 = cond ? f : wd;
        Object k106 = cond ? f : wbo;
        Object k107 = cond ? f : o;
        Object k108 = cond ? f : null;
        Object k109 = cond ? d : b;
        Object k110 = cond ? d : s;
        Object k111 = cond ? d : c;
        Object k112 = cond ? d : i;
        Object k113 = cond ? d : l;
        Object k114 = cond ? d : f;
        Object k115 = cond ? d : d;
        Object k116 = cond ? d : bo;
        Object k117 = cond ? d : wb;
        Object k118 = cond ? d : ws;
        Object k119 = cond ? d : wc;
        Object k120 = cond ? d : wi;
        Object k121 = cond ? d : wl;
        Object k122 = cond ? d : wf;
        Object k123 = cond ? d : wd;
        Object k124 = cond ? d : wbo;
        Object k125 = cond ? d : o;
        Object k126 = cond ? d : null;
        Object k127 = cond ? bo : b;
        Object k128 = cond ? bo : s;
        Object k129 = cond ? bo : c;
        Object k130 = cond ? bo : i;
        Object k131 = cond ? bo : l;
        Object k132 = cond ? bo : f;
        Object k133 = cond ? bo : d;
        Object k134 = cond ? bo : bo;
        Object k135 = cond ? bo : wb;
        Object k136 = cond ? bo : ws;
        Object k137 = cond ? bo : wc;
        Object k138 = cond ? bo : wi;
        Object k139 = cond ? bo : wl;
        Object k140 = cond ? bo : wf;
        Object k141 = cond ? bo : wd;
        Object k142 = cond ? bo : wbo;
        Object k143 = cond ? bo : o;
        Object k144 = cond ? bo : null;
        Object k145 = cond ? wb : b;
        Object k146 = cond ? wb : s;
        Object k147 = cond ? wb : c;
        Object k148 = cond ? wb : i;
        Object k149 = cond ? wb : l;
        Object k150 = cond ? wb : f;
        Object k151 = cond ? wb : d;
        Object k152 = cond ? wb : bo;
        Object k153 = cond ? wb : wb;
        Object k154 = cond ? wb : ws;
        Object k155 = cond ? wb : wc;
        Object k156 = cond ? wb : wi;
        Object k157 = cond ? wb : wl;
        Object k158 = cond ? wb : wf;
        Object k159 = cond ? wb : wd;
        Object k160 = cond ? wb : wbo;
        Object k161 = cond ? wb : o;
        Object k162 = cond ? wb : null;
        Object k163 = cond ? ws : b;
        Object k164 = cond ? ws : s;
        Object k165 = cond ? ws : c;
        Object k166 = cond ? ws : i;
        Object k167 = cond ? ws : l;
        Object k168 = cond ? ws : f;
        Object k169 = cond ? ws : d;
        Object k170 = cond ? ws : bo;
        Object k171 = cond ? ws : wb;
        Object k172 = cond ? ws : ws;
        Object k173 = cond ? ws : wc;
        Object k174 = cond ? ws : wi;
        Object k175 = cond ? ws : wl;
        Object k176 = cond ? ws : wf;
        Object k177 = cond ? ws : wd;
        Object k178 = cond ? ws : wbo;
        Object k179 = cond ? ws : o;
        Object k180 = cond ? ws : null;
        Object k181 = cond ? wc : b;
        Object k182 = cond ? wc : s;
        Object k183 = cond ? wc : c;
        Object k184 = cond ? wc : i;
        Object k185 = cond ? wc : l;
        Object k186 = cond ? wc : f;
        Object k187 = cond ? wc : d;
        Object k188 = cond ? wc : bo;
        Object k189 = cond ? wc : wb;
        Object k190 = cond ? wc : ws;
        Object k191 = cond ? wc : wc;
        Object k192 = cond ? wc : wi;
        Object k193 = cond ? wc : wl;
        Object k194 = cond ? wc : wf;
        Object k195 = cond ? wc : wd;
        Object k196 = cond ? wc : wbo;
        Object k197 = cond ? wc : o;
        Object k198 = cond ? wc : null;
        Object k199 = cond ? wi : b;
        Object k200 = cond ? wi : s;
        Object k201 = cond ? wi : c;
        Object k202 = cond ? wi : i;
        Object k203 = cond ? wi : l;
        Object k204 = cond ? wi : f;
        Object k205 = cond ? wi : d;
        Object k206 = cond ? wi : bo;
        Object k207 = cond ? wi : wb;
        Object k208 = cond ? wi : ws;
        Object k209 = cond ? wi : wc;
        Object k210 = cond ? wi : wi;
        Object k211 = cond ? wi : wl;
        Object k212 = cond ? wi : wf;
        Object k213 = cond ? wi : wd;
        Object k214 = cond ? wi : wbo;
        Object k215 = cond ? wi : o;
        Object k216 = cond ? wi : null;
        Object k217 = cond ? wl : b;
        Object k218 = cond ? wl : s;
        Object k219 = cond ? wl : c;
        Object k220 = cond ? wl : i;
        Object k221 = cond ? wl : l;
        Object k222 = cond ? wl : f;
        Object k223 = cond ? wl : d;
        Object k224 = cond ? wl : bo;
        Object k225 = cond ? wl : wb;
        Object k226 = cond ? wl : ws;
        Object k227 = cond ? wl : wc;
        Object k228 = cond ? wl : wi;
        Object k229 = cond ? wl : wl;
        Object k230 = cond ? wl : wf;
        Object k231 = cond ? wl : wd;
        Object k232 = cond ? wl : wbo;
        Object k233 = cond ? wl : o;
        Object k234 = cond ? wl : null;
        Object k235 = cond ? wf : b;
        Object k236 = cond ? wf : s;
        Object k237 = cond ? wf : c;
        Object k238 = cond ? wf : i;
        Object k239 = cond ? wf : l;
        Object k240 = cond ? wf : f;
        Object k241 = cond ? wf : d;
        Object k242 = cond ? wf : bo;
        Object k243 = cond ? wf : wb;
        Object k244 = cond ? wf : ws;
        Object k245 = cond ? wf : wc;
        Object k246 = cond ? wf : wi;
        Object k247 = cond ? wf : wl;
        Object k248 = cond ? wf : wf;
        Object k249 = cond ? wf : wd;
        Object k250 = cond ? wf : wbo;
        Object k251 = cond ? wf : o;
        Object k252 = cond ? wf : null;
        Object k253 = cond ? wd : b;
        Object k254 = cond ? wd : s;
        Object k255 = cond ? wd : c;
        Object k256 = cond ? wd : i;
        Object k257 = cond ? wd : l;
        Object k258 = cond ? wd : f;
        Object k259 = cond ? wd : d;
        Object k260 = cond ? wd : bo;
        Object k261 = cond ? wd : wb;
        Object k262 = cond ? wd : ws;
        Object k263 = cond ? wd : wc;
        Object k264 = cond ? wd : wi;
        Object k265 = cond ? wd : wl;
        Object k266 = cond ? wd : wf;
        Object k267 = cond ? wd : wd;
        Object k268 = cond ? wd : wbo;
        Object k269 = cond ? wd : o;
        Object k270 = cond ? wd : null;
        Object k271 = cond ? wbo : b;
        Object k272 = cond ? wbo : s;
        Object k273 = cond ? wbo : c;
        Object k274 = cond ? wbo : i;
        Object k275 = cond ? wbo : l;
        Object k276 = cond ? wbo : f;
        Object k277 = cond ? wbo : d;
        Object k278 = cond ? wbo : bo;
        Object k279 = cond ? wbo : wb;
        Object k280 = cond ? wbo : ws;
        Object k281 = cond ? wbo : wc;
        Object k282 = cond ? wbo : wi;
        Object k283 = cond ? wbo : wl;
        Object k284 = cond ? wbo : wf;
        Object k285 = cond ? wbo : wd;
        Object k286 = cond ? wbo : wbo;
        Object k287 = cond ? wbo : o;
        Object k288 = cond ? wbo : null;
        Object k289 = cond ? o : b;
        Object k290 = cond ? o : s;
        Object k291 = cond ? o : c;
        Object k292 = cond ? o : i;
        Object k293 = cond ? o : l;
        Object k294 = cond ? o : f;
        Object k295 = cond ? o : d;
        Object k296 = cond ? o : bo;
        Object k297 = cond ? o : wb;
        Object k298 = cond ? o : ws;
        Object k299 = cond ? o : wc;
        Object k300 = cond ? o : wi;
        Object k301 = cond ? o : wl;
        Object k302 = cond ? o : wf;
        Object k303 = cond ? o : wd;
        Object k304 = cond ? o : wbo;
        Object k305 = cond ? o : o;
        Object k306 = cond ? o : null;
        Object k307 = cond ? null : b;
        Object k308 = cond ? null : s;
        Object k309 = cond ? null : c;
        Object k310 = cond ? null : i;
        Object k311 = cond ? null : l;
        Object k312 = cond ? null : f;
        Object k313 = cond ? null : d;
        Object k314 = cond ? null : bo;
        Object k315 = cond ? null : wb;
        Object k316 = cond ? null : ws;
        Object k317 = cond ? null : wc;
        Object k318 = cond ? null : wi;
        Object k319 = cond ? null : wl;
        Object k320 = cond ? null : wf;
        Object k321 = cond ? null : wd;
        Object k322 = cond ? null : wbo;
        Object k323 = cond ? null : o;
        Object k324 = cond ? null : null;
    }
}
