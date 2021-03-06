SELECT tab0.v1 AS v1 , tab4.v5 AS v5 , tab5.v6 AS v6 , tab3.v4 AS v4 , tab2.v3 AS v3 , tab1.v2 AS v2 
 FROM    (SELECT obj AS v1 
	 FROM gr__offers$$1$$ 
	 WHERE sub = 'wsdbm:Retailer852'
	) tab0
 JOIN    (SELECT sub AS v1 , obj AS v2 
	 FROM gr__includes$$2$$
	) tab1
 ON(tab0.v1=tab1.v1)
 JOIN    (SELECT obj AS v3 , sub AS v2 
	 FROM rev__hasReview$$3$$
	
	) tab2
 ON(tab1.v2=tab2.v2)
 JOIN    (SELECT obj AS v4 , sub AS v3 
	 FROM rev__reviewer$$4$$
	
	) tab3
 ON(tab2.v3=tab3.v3)
 JOIN    (SELECT obj AS v5 , sub AS v4 
	 FROM wsdbm__likes$$5$$
	) tab4
 ON(tab3.v4=tab4.v4)
 JOIN    (SELECT sub AS v5 , obj AS v6 
	 FROM sorg__description$$6$$
	
	) tab5
 ON(tab4.v5=tab5.v5)


++++++Tables Statistic
sorg__description$$6$$	0	VP	sorg__description/
	VP	<sorg__description>	14960
------
gr__offers$$1$$	0	VP	gr__offers/
	VP	<gr__offers>	119316
------
rev__reviewer$$4$$	0	VP	rev__reviewer/
	VP	<rev__reviewer>	150000
------
gr__includes$$2$$	0	VP	gr__includes/
	VP	<gr__includes>	90000
------
wsdbm__likes$$5$$	0	VP	wsdbm__likes/
	VP	<wsdbm__likes>	112401
------
rev__hasReview$$3$$	0	VP	rev__hasReview/
	VP	<rev__hasReview>	149634
------
