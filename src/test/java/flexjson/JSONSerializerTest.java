/**
 * Copyright 2007 Charlie Hubbard and Brandon Goodin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package flexjson;

import flexjson.mock.*;
import flexjson.transformer.AbstractTransformer;
import flexjson.transformer.DateTransformer;
import flexjson.transformer.HtmlEncoderTransformer;
import flexjson.model.ListContainer;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JSONSerializerTest {

    final Logger logger = LoggerFactory.getLogger(SimpleSerializeTest.class);

    private Person charlie, ben, pedro;
    private Map colors;
    private List people;
    private Network network;
    private Zipcode pedroZip;
    private Employee dilbert;

    @SuppressWarnings({"unchecked"})
    @Before
    public void setUp() {
        FixtureCreator fixtureCreator = new FixtureCreator();
        pedroZip = new Zipcode("49404");
        pedro = fixtureCreator.createPedro();
        charlie = fixtureCreator.createCharlie();
        ben = fixtureCreator.createBen();
        colors = fixtureCreator.createColorMap();

        people = new ArrayList();
        people.add(charlie);
        people.add(ben);
        people.add(pedro);

        dilbert = fixtureCreator.createDilbert();

        network = fixtureCreator.createNetwork("My Network", charlie, ben );
    }

    @Test
    public void testObject() {
        JSONSerializer serializer = new JSONSerializer();

        String charlieJson = serializer.serialize(charlie);

        assertStringValue(Person.class.getName(), charlieJson);
        assertAttribute("firstname", charlieJson);
        assertStringValue("Charlie", charlieJson);
        assertAttribute("lastname", charlieJson);
        assertStringValue("Hubbard", charlieJson);
        assertAttribute("work", charlieJson);
        assertAttribute("home", charlieJson);
        assertAttribute("street", charlieJson);
        assertStringValue(Address.class.getName(), charlieJson);
        assertAttribute("zipcode", charlieJson);
        assertStringValue(Zipcode.class.getName(), charlieJson);
        assertAttributeMissing("person", charlieJson);

        assertAttributeMissing("phones", charlieJson);
        assertStringValueMissing(Phone.class.getName(), charlieJson);
        assertAttributeMissing("hobbies", charlieJson);

        JSONSerializer benSerializer = new JSONSerializer();
        benSerializer.exclude("home", "work");
        String benJson = benSerializer.serialize(ben);
        assertStringValue(Person.class.getName(), benJson);
        assertAttribute("firstname", benJson);
        assertStringValue("Ben", benJson);
        assertAttribute("lastname", benJson);
        assertStringValue("Hubbard", benJson);
        assertAttribute("birthdate", benJson);

        assertStringValueMissing(Address.class.getName(), benJson);
        assertAttributeMissing("work", benJson);
        assertAttributeMissing("home", benJson);
        assertAttributeMissing("street", benJson);
        assertAttributeMissing("city", benJson);
        assertAttributeMissing("state", benJson);
        assertStringValueMissing(Zipcode.class.getName(), benJson);
        assertAttributeMissing("zipcode", benJson);
        assertStringValueMissing(Phone.class.getName(), benJson);
        assertAttributeMissing("hobbies", benJson);
        assertAttributeMissing("person", benJson);

        serializer.exclude("home.zipcode", "work.zipcode");

        String json2 = serializer.serialize(charlie, new StringBuilder());
        assertStringValue(Person.class.getName(), json2);
        assertAttribute("work", json2);
        assertAttribute("home", json2);
        assertAttribute("street", json2);
        assertStringValue(Address.class.getName(), json2);
        assertAttributeMissing("zipcode", json2);
        assertAttributeMissing("phones", json2);
        assertStringValueMissing(Zipcode.class.getName(), json2);
        assertStringValueMissing(Phone.class.getName(), json2);
        assertAttributeMissing("hobbies", json2);
        assertAttributeMissing("type", json2);
        assertStringValueMissing("PAGER", json2);

        serializer.include("hobbies").exclude("phones.areaCode", "phones.exchange", "phones.number");

        String json3 = serializer.serialize(charlie, new StringBuilder());
        assertStringValue(Person.class.getName(), json3);
        assertAttribute("work", json3);
        assertAttribute("home", json3);
        assertAttribute("street", json3);
        assertStringValue(Address.class.getName(), json3);
        assertAttribute("phones", json3);
        assertAttribute("phoneNumber", json3);
        assertStringValue(Phone.class.getName(), json3);
        assertAttribute("hobbies", json3);

        assertAttributeMissing("zipcode", json3);
        assertAttributeMissing(Zipcode.class.getName(), json3);
        assertAttributeMissing("areaCode", json3);
        assertAttributeMissing("exchange", json3);
        assertAttributeMissing("number", json3);
        assertAttribute("type", json3);
        assertStringValue("PAGER", json3);

        assertTrue(json3.startsWith("{"));
        assertTrue(json3.endsWith("}"));
    }

    @SuppressWarnings({"ForLoopReplaceableByForEach"})
    @Test
    public void testMap() {
        JSONSerializer serializer = new JSONSerializer();
        String colorsJson = serializer.serialize(colors);
        for (Iterator i = colors.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            assertAttribute(entry.getKey().toString(), colorsJson);
            assertStringValue(entry.getValue().toString(), colorsJson);
        }
        assertTrue(colorsJson.startsWith("{"));
        assertTrue(colorsJson.endsWith("}"));

        colors.put( null, "#aaaaaa" );
        colors.put( "orange", null );

        String json = serializer.serialize( colors );
        assertTrue( "Assert null is present as a key", json.contains("null:") );
        assertStringValue( "#aaaaaa", json );
        assertAttribute( "orange", json );
        assertTrue( "Assert null is present as a value", json.contains( ":null" ) );
    }

	@Test
	public void should_give_different_null_value_for_specific_propery() {
		JSONSerializer serializer = new JSONSerializer().exclude("*.class").exclude("toString").exclude("hashCode");
		serializer.transform(new AbstractTransformer() {
			public void transform(Object data) {
				boolean setContext = false;
				TypeContext typeContext = getContext().peekTypeContext();
				String propertyName = typeContext != null ? typeContext.getPropertyName() : "";

				if (typeContext == null || typeContext.getBasicType() != BasicType.OBJECT) {
					typeContext = getContext().writeOpenObject();
					setContext = true;
				}

				try {
					if (!typeContext.isFirst()) {
						getContext().writeComma();
					}
					getContext().writeName(propertyName);

					getContext().writeOpenObject();

					getContext().writeName("value");
					getContext().write((data == null) ? null : data.toString());

					getContext().writeComma();
					getContext().writeName("displayStr");
					getContext().writeQuoted(data == null ? "No Data" : data +  "&deg;F");

					getContext().writeCloseObject();

				} catch (Throwable e) {
					System.out.println("Failed to transform parameter: " + propertyName + ", value --> " + data + " --> " + e);
				} finally {
					if (setContext) {
						getContext().writeCloseObject();
					}
				}
			}

			@Override
			public Boolean isInline() {
				return Boolean.TRUE;
			}
		}, "lastname");

		FixtureCreator fixtureCreator = new FixtureCreator();
		Person lCharlie = fixtureCreator.createCharlie();
		lCharlie.setLastname(null);
		String json = serializer.deepSerialize(lCharlie);
		System.out.println(json);
	}

    @Test
    public void testArray() {
        int[] array = new int[30];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }

        String json = new JSONSerializer().serialize(array);

        for (int i = 0; i < array.length; i++) {
            assertNumber(i, json);
        }

        assertFalse("Assert that there are no double quotes in the output", json.contains("\""));
        assertFalse("Assert that there are no single quotes in the output", json.contains("\'"));
    }

    @SuppressWarnings({"ForLoopReplaceableByForEach"})
    @Test
    public void testCollection() {
        JSONSerializer serializer = new JSONSerializer();
        String colorsJson = serializer.serialize(colors.values());
        for (Iterator i = colors.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            assertAttributeMissing(entry.getKey().toString(), colorsJson);
            assertStringValue(entry.getValue().toString(), colorsJson);
        }
        assertTrue(colorsJson.startsWith("["));
        assertTrue(colorsJson.endsWith("]"));
    }

    @Test
    public void testString() {
        assertSerializedTo("Hello", "\"Hello\"");
        assertSerializedTo("Hello World", "\"Hello World\"");
        assertSerializedTo("Hello\nWorld", "\"Hello\\nWorld\"");
        assertSerializedTo("Hello 'Charlie'", "\"Hello \\u0027Charlie\\u0027\"");
        assertSerializedTo("Hello \"Charlie\"", "\"Hello \\u0022Charlie\\u0022\"");
        assertSerializedTo("</script>", "\"\\u003c/script\\u003e\"");
        assertSerializedTo(
                "� Shadowing the senior pastor as he performed weekly duties including sermon\n" +
                "preparation, wedding, funerals, and other activities.\n" +
                "� Teaching Junior High School Sunday School.\n" +
                "� Participating in session meetings, worship planning meetings, and staff meetings.\n" +
                "� Assisting in research for sermon preparation.\n" +
                "� Speaking occasionally in church including scripture reading and giving the\n" +
                "announcements.",
                "\"� Shadowing the senior pastor as he performed weekly duties including sermon\\n" +
                "preparation, wedding, funerals, and other activities.\\n" +
                "� Teaching Junior High School Sunday School.\\n" +
                "� Participating in session meetings, worship planning meetings, and staff meetings.\\n" +
                "� Assisting in research for sermon preparation.\\n" +
                "� Speaking occasionally in church including scripture reading and giving the\\n" +
                "announcements.\"");
        Map test = new HashMap();
        test.put("</script>", "</script>");
        assertEquals("{\"\\u003c/script\\u003e\":\"\\u003c/script\\u003e\"}", new JSONSerializer().serialize(test));
    }

    @Test
    public void testListOfObjects() {
        JSONSerializer serializer = new JSONSerializer();
        String peopleJson = serializer.serialize(people);

        assertStringValue(Person.class.getName(), peopleJson);
        assertAttribute("firstname", peopleJson);
        assertStringValue("Charlie", peopleJson);
        assertStringValue("Ben", peopleJson);
        assertAttribute("lastname", peopleJson);
        assertStringValue("Hubbard", peopleJson);
        assertStringValue(Address.class.getName(), peopleJson);
        assertStringValue("Pedro", peopleJson);
        assertStringValue("Neves", peopleJson);

        serializer = new JSONSerializer().exclude("home", "work");
        peopleJson = serializer.serialize(people);

        assertStringValue(Person.class.getName(), peopleJson);
        assertAttribute("firstname", peopleJson);
        assertStringValue("Charlie", peopleJson);
        assertStringValue("Ben", peopleJson);
        assertAttribute("lastname", peopleJson);
        assertStringValue("Hubbard", peopleJson);
        assertStringValueMissing(Address.class.getName(), peopleJson);
    }

    @Test
    public void testDeepIncludes() {
        JSONSerializer serializer = new JSONSerializer();
        String peopleJson = serializer.include("people.hobbies").serialize(network);

        assertAttribute("name", peopleJson);
        assertStringValue("My Network", peopleJson);
        assertAttribute("firstname", peopleJson);
        assertStringValue("Charlie", peopleJson);
        assertStringValue("Ben", peopleJson);
        assertAttribute("lastname", peopleJson);
        assertStringValue("Hubbard", peopleJson);
        assertAttribute("hobbies", peopleJson);
        assertStringValue("Purse snatching", peopleJson);
    }

    @Test
    public void testDates() {
        JSONSerializer serializer = new JSONSerializer();
        String peopleJson = serializer.exclude("home", "work").serialize(charlie);
        assertAttribute("firstname", peopleJson);
        assertStringValue("Charlie", peopleJson);
        assertNumber(charlie.getBirthdate().getTime(), peopleJson);
        assertStringValueMissing("java.util.Date", peopleJson);
    }

    @Test
    public void testSimpleShallowWithListInMap() {
        JSONSerializer serializer = new JSONSerializer();
        Map wrapper = new HashMap();
        wrapper.put("name","Joe Blow");
        wrapper.put("people",people);
        String peopleJson = serializer.serialize(wrapper);
        logger.info(peopleJson);
        assertFalse(peopleJson.contains("["));
    }

    @Test
    public void testSimpleShallowWithListInObject() {
        JSONSerializer serializer = new JSONSerializer();
        ListContainer wrapper = new ListContainer();
        wrapper.setName("Joe Blow");
        wrapper.setPeople(people);
        String peopleJson = serializer.serialize(wrapper);
        logger.info(peopleJson);
        assertFalse(peopleJson.contains("["));
    }

    @Test
    public void testRootName() {
        JSONSerializer serializer = new JSONSerializer().rootName("people");
        String peopleJson = serializer.serialize(people);
        logger.info(peopleJson);
        assertTrue(peopleJson.startsWith("{\"people\":"));
    }

    @Test
    public void testSetIncludes() {
        JSONSerializer serializer = new JSONSerializer();
        serializer.setIncludes(Arrays.asList("people.hobbies", "phones", "home", "people.resume"));
        List<PathExpression> includes = serializer.getIncludes();

        assertFalse(includes.isEmpty());
        assertEquals(4, includes.size());
        assertTrue(includes.contains(new PathExpression("people.hobbies", true)));
        assertTrue(includes.contains(new PathExpression("people.resume", true)));
        assertTrue(includes.contains(new PathExpression("phones", true)));
        assertTrue(includes.contains(new PathExpression("home", true)));
    }

    @Test
    public void testI18n() {
        JSONSerializer serializer = new JSONSerializer();
        String json = serializer.include("work", "home").serialize(pedro);

        assertAttribute("work", json);
        assertAttribute("home", json);

        assertEquals(2, occurs("Acrel\u00E8ndia", json));
    }

    @Test
    public void testDeepSerialization() {
        JSONSerializer serializer = new JSONSerializer();
        String peopleJson = serializer.deepSerialize(network);

        assertAttribute("name", peopleJson);
        assertStringValue("My Network", peopleJson);
        assertAttribute("firstname", peopleJson);
        assertStringValue("Charlie", peopleJson);
        assertStringValue("Ben", peopleJson);
        assertAttribute("lastname", peopleJson);
        assertStringValue("Hubbard", peopleJson);
        assertAttributeMissing("hobbies", peopleJson); // there is an annotation that explicitly excludes this!
        assertStringValueMissing("Purse snatching", peopleJson);
    }

    @Test
    public void testDeepSerializationWithIncludeOverrides() {
        JSONSerializer serializer = new JSONSerializer();
        String peopleJson = serializer.include("people.hobbies").deepSerialize(network);

        assertAttribute("firstname", peopleJson);
        assertStringValue("Charlie", peopleJson);
        assertAttribute("hobbies", peopleJson);
        assertStringValue("Purse snatching", peopleJson);
        assertStringValue("Running sweat shops", peopleJson);
        assertStringValue("Fixing prices", peopleJson);
    }

    @Test
    public void testDeepSerializationWithExcludes() {
        JSONSerializer serializer = new JSONSerializer();
        String peopleJson = serializer.exclude("people.work").deepSerialize(network);

        assertAttribute("firstname", peopleJson);
        assertStringValue("Charlie", peopleJson);
        assertAttributeMissing("work", peopleJson);
        assertStringValue("4132 Pluto Drive", peopleJson);
        assertAttribute("home", peopleJson);
        assertAttribute("phones", peopleJson);
    }

    @Test
    public void testDeepSerializationCycles() {
        JSONSerializer serializer = new JSONSerializer();
        String json = serializer.deepSerialize(people);

        assertAttribute("zipcode", json);
        assertEquals(2, occurs(pedroZip.getZipcode(), json));
        assertAttributeMissing("person", json);
    }

    @Test
    public void testSerializeSuperClass() {
        JSONSerializer serializer = new JSONSerializer();
        String json = serializer.serialize(dilbert);

        assertAttribute("company", json);
        assertStringValue("Initech", json);
        assertAttribute("firstname", json);
        assertStringValue("Dilbert", json);
    }

    @Test
    public void testSerializePublicFields() {
        Spiderman spiderman = new Spiderman();

        JSONSerializer serializer = new JSONSerializer();
        String json = serializer.serialize(spiderman);

        assertAttribute("spideySense", json);
        assertAttribute("superpower", json);
        assertStringValue("Creates web", json);
    }

    /**
     * https://sourceforge.net/tracker/index.php?func=detail&aid=2927626&group_id=194042&atid=947842#
     */
    @Test
    public void testExcludingPublicFields() {
        Spiderman spiderman = new Spiderman();

        String json = new JSONSerializer().exclude("superpower").serialize( spiderman );

        assertAttributeMissing("superpower", json);
        assertAttribute("spideySense", json);
    }

    @Test
    public void testPrettyPrint() {
        JSONSerializer serializer = new JSONSerializer();

        serializer.include("phones").prettyPrint(true);
        String charlieJson = serializer.serialize(charlie);
        logger.info(charlieJson);
    }

    @Test
    public void testWildcards() {
        JSONSerializer serializer = new JSONSerializer();
        String json = serializer.include("phones").exclude("*.class").serialize(charlie);

        assertAttributeMissing("class", json);
        assertAttribute("phones", json);
        assertAttributeMissing("hobbies", json);
    }

    @Test
    public void testWildcardDepthControl() {
        JSONSerializer serializer = new JSONSerializer();
        serializer.include("*.class").prettyPrint(true);
        String json = serializer.serialize(charlie);

        assertAttributeMissing("phones", json);
        assertAttributeMissing("hobbies", json);
    }

    @Test
    public void testExclude() {
        String json = new JSONSerializer().serialize(charlie);

        assertAttribute("firstname", json);
        assertAttributeMissing("number", json);
        assertAttributeMissing("exchange", json);
        assertAttributeMissing("areaCode", json);

        json = new JSONSerializer().include("phones").serialize(charlie);

        assertAttribute("firstname", json);
        assertAttribute("number", json);
        assertAttribute("exchange", json);
        assertAttribute("areaCode", json);

        json = new JSONSerializer().exclude("phones.areaCode").serialize(charlie);

        assertAttribute("firstname", json);
        assertAttribute("number", json);
        assertAttribute("exchange", json);
        assertAttributeMissing("areaCode", json);
    }

    @Ignore
    @Test
    public void testExcludeWithMap() {
        TestClass2 test = new TestClass2();
        test.getMapOfJustice().put("something", new TestClass3("Germany", "Europe", true) );
        test.getMapOfJustice().put("something2", new TestClass3("China", "Asia", false) );
        test.getMapOfJustice().put("something3", new TestClass3("Australia", "Australia", true) );

        String json = new JSONSerializer().exclude("mapOfJustice.*.category").prettyPrint(true).serialize(test);

        assertAttribute("mapOfJustice", json);
        assertAttribute("name", json);
        assertAttribute("found", json);
        assertAttributeMissing("category", json);
    }

    @Test
    public void testExcludeAll() {
        JSONSerializer serializer = new JSONSerializer();
        String json = serializer.exclude("*").serialize(charlie);

        assertEquals("{}", json);
        assertAttributeMissing("class", json);
        assertAttributeMissing("phones", json);
        assertAttributeMissing("firstname", json);
        assertAttributeMissing("lastname", json);
        assertAttributeMissing("hobbies", json);
    }

    @Test
    public void testMixedWildcards() {
        JSONSerializer serializer = new JSONSerializer();
        serializer.include("firstname", "lastname").exclude("*").prettyPrint(true);
        String json = serializer.serialize(charlie);

        assertAttribute("firstname", json);
        assertStringValue("Charlie", json);
        assertAttribute("lastname", json);
        assertStringValue("Hubbard", json);
        assertAttributeMissing("class", json);
        assertAttributeMissing("phones", json);
        assertAttributeMissing("birthdate", json);

        serializer = new JSONSerializer();
        serializer.include("firstname", "lastname", "phones.areaCode", "phones.exchange", "phones.number").exclude("*").prettyPrint(true);
        json = serializer.serialize(charlie);

        assertAttribute("firstname", json);
        assertStringValue("Charlie", json);
        assertAttribute("lastname", json);
        assertStringValue("Hubbard", json);
        assertAttributeMissing("class", json);
        assertAttribute("phones", json);
        assertAttributeMissing("birthdate", json);
    }

    @Test
    public void testHtmlTransformation() {
        String json = new JSONSerializer().transform(new HtmlEncoderTransformer(), "").serialize("Marker & Thompson");
        assertEquals("Assert that the & was replaced with &amp;", "\"Marker &amp; Thompson\"", json);

        Map<String, String> map = new HashMap<String, String>();
        map.put("Chuck D", "Chuck D <chuckd@publicenemy.com>");
        map.put("Run", "Run <run@rundmc.com>");
        json = new JSONSerializer().transform(new HtmlEncoderTransformer(), String.class).serialize(map);
        assertStringValue("Chuck D &lt;chuckd@publicenemy.com&gt;", json);
        assertStringValue("Run &lt;run@rundmc.com&gt;", json);

        Person xeno = new Person("><eno", "h&d", new Date(), new Address("1092 Hemphill", "Atlanta", "GA", new Zipcode("30319")), new Address("333 \"Diddle & Town\"", "Atlanta", "30329", new Zipcode("30320")));

        json = new JSONSerializer().transform(new HtmlEncoderTransformer(), "firstname", "lastname").exclude("*.class").serialize(xeno);

        assertStringValue("&gt;&lt;eno", json);
        assertStringValue("h&amp;d", json);
        assertStringValue("333 \\u0022Diddle \\u0026 Town\\u0022", json);
        assertStringValueMissing("333 &quot;Diddle &amp; Town&quot;", json);
        assertAttributeMissing("class", json);
    }

    @Test
    public void testDateTransforming() {
        String json = new JSONSerializer().transform(new DateTransformer("yyyy-MM-dd"), "birthdate").serialize(charlie);

        assertAttribute("birthdate", json);
        assertStringValue("1988-11-23", json);
    }

    @Test
    public void testCopyOnWriteList() {
        CopyOnWriteArrayList<Person> people = new CopyOnWriteArrayList<Person>();
        people.add( charlie );
        people.add( ben );

        String json = new JSONSerializer().serialize( people );
        assertAttribute("firstname", json );
        assertStringValue("Charlie", json );
        assertStringValue("Ben", json );
    }

    @Test
    public void testAnnotations() {
        HashMap<String, TestClass3> map = new HashMap<String, TestClass3>();
        map.put("String1", new TestClass3());

        TestClass2 testElement = new TestClass2();
        testElement.setMapOfJustice(map);

        String json = new JSONSerializer().serialize( testElement );
        assertAttributeMissing("mapOfJustice", json);
        assertAttributeMissing("name", json);
        assertEquals(-1, json.indexOf("testName2"));

        json = new JSONSerializer().include("mapOfJustice").serialize( testElement );
        assertAttribute("mapOfJustice", json);
        // make sure the name property value is missing!  assertAttributeMissing( "name", json )
        // conflicts since mapOfJustice contains an object with name in it
        assertEquals(-1, json.indexOf("testName2") );
    }

    @Test
    public void testTransient() {
        TestClass2 testElement = new TestClass2();

        String json = new JSONSerializer().serialize( testElement );
        assertAttributeMissing("description", json);

        json = new JSONSerializer().include("description").serialize( testElement );
        assertAttribute("description", json);
    }

    @Test
    public void testSettersWithoutGettersAreMissing() {
        Friend friend = new Friend("Nugget", "Donkey Rider", "Slim");
        String json = new JSONSerializer().include("*").prettyPrint(true).serialize( friend );
        assertAttribute("nicknames", json);
        assertAttributeMissing("nicknamesAsArray", json);
    }

    @Test
    public void testCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));

        cal.set(Calendar.YEAR, 1970);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.AM_PM, Calendar.AM);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Map<String,Calendar> target = new HashMap<String, Calendar>();
        target.put("epoch", cal);

        String json = new JSONSerializer().serialize( target );

        Assert.assertEquals("{\"epoch\":0}", json );
    }

    private int occurs(String str, String json) {
        int current = 0;
        int count = 0;
        while (current >= 0) {
            current = json.indexOf(str, current);
            if (current > 0) {
                count++;
                current += str.length();
            }
        }
        return count;
    }

    private void assertAttributeMissing(String attribute, String json) {
        assertAttribute(attribute, json, false);
    }

    private void assertAttribute(String attribute, String peopleJson) {
        assertAttribute(attribute, peopleJson, true);
    }

    private void assertAttribute(String attribute, String peopleJson, boolean isPresent) {
        if (isPresent) {
            assertTrue("'" + attribute + "' attribute is missing", peopleJson.contains("\"" + attribute + "\":"));
        } else {
            assertFalse("'" + attribute + "' attribute is present when it's not expected.", peopleJson.contains("\"" + attribute + "\":"));
        }
    }

    private void assertStringValue(String value, String json, boolean isPresent) {
        if (isPresent) {
            assertTrue("'" + value + "' value is missing", json.contains("\"" + value + "\""));
        } else {
            assertFalse("'" + value + "' value is present when it's not expected.", json.contains("\"" + value + "\""));
        }
    }

    private void assertNumber(Number number, String json) {
        assertTrue(number + " is missing as a number.", json.contains(number.toString()));
    }

    private void assertStringValueMissing(String value, String json) {
        assertStringValue(value, json, false);
    }

    private void assertStringValue(String value, String json) {
        assertStringValue(value, json, true);
    }

    private void assertSerializedTo(String original, String expected) {
        JSONSerializer serializer = new JSONSerializer();
        String json = serializer.serialize(original);
        assertEquals(expected, json);
    }

    @After
    public void tearDown() {
    }

}
