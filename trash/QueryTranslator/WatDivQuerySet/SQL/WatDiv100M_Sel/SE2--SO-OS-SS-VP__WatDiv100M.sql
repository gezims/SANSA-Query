SELECT tab0.v1 AS v1 , tab0.v0 AS v0 , tab2.v3 AS v3 , tab1.v2 AS v2 
 FROM    (SELECT obj AS v1 , sub AS v0 
	 FROM mo__artist$$1$$
	) tab0
 JOIN    (SELECT sub AS v1 , obj AS v2 
	 FROM wsdbm__friendOf$$2$$
	
	) tab1
 ON(tab0.v1=tab1.v1)
 JOIN    (SELECT obj AS v3 , sub AS v2 
	 FROM wsdbm__follows$$3$$
	
	) tab2
 ON(tab1.v2=tab2.v2)


++++++Tables Statistic
wsdbm__friendOf$$2$$	1	SO	wsdbm__friendOf/mo__artist
	VP	<wsdbm__friendOf>	45092208
	SO	<wsdbm__friendOf><mo__artist>	266771	0.01
	OS	<wsdbm__friendOf><wsdbm__follows>	34944355	0.77
------
wsdbm__follows$$3$$	0	VP	wsdbm__follows/
	VP	<wsdbm__follows>	32736135
	SO	<wsdbm__follows><wsdbm__friendOf>	32736135	1.0
------
mo__artist$$1$$	1	OS	mo__artist/wsdbm__friendOf
	VP	<mo__artist>	13306
	OS	<mo__artist><wsdbm__friendOf>	5227	0.39
------
