package ch.eugster.swissqrbill.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.eugster.swissqrbill.SwissQRBillGenerator;
import net.codecrete.qrbill.generator.GraphicsFormat;
import net.codecrete.qrbill.generator.Language;
import net.codecrete.qrbill.generator.OutputSize;

public class QRCodeTest 
{
	private String output;

	private String invoice;

	@BeforeEach
	public void beforeEach() throws URISyntaxException
	{
		String out = (System.getProperty("user.home") + File.separator + UUID.randomUUID() + ".pdf");
		output = new File(out).toURI().toASCIIString();
		URI uri = QRCodeTest.class.getResource("/invoice.pdf").toURI();
		invoice = uri.toASCIIString();
	}

	@AfterEach
	public void afterEach()
	{
		Path path = null;
		try
		{
			URI uri = new URI(output);
			path = Paths.get(uri);
		}
		catch (Exception e)
		{
			path = Paths.get(output);
		}
		if (path != null && path.toFile().exists())
		{
			path.toFile().delete();
		}
	}
	
	@Test
	public void showVolumes()
	{
		File file = null;
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			file = new File("C:/Users/christian/");
			assertTrue(file.exists());
			assertTrue(file.isAbsolute());
			assertTrue(file.isDirectory());
		}
		else if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
		{
			file = new File("/Festplatte/Users/christian/");
			assertFalse(file.exists());
			assertTrue(file.isAbsolute());
			assertFalse(file.isDirectory());
			file = new File("/Volumes" + file.getAbsolutePath());
			assertTrue(file.exists());
			assertTrue(file.isAbsolute());
			assertTrue(file.isDirectory());
		}
	}
	
	@Test
	public void testWithInvoiceAsNumberAndDebtorNumber() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", this.output);
		path.put("invoice", this.invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testWithReference() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", output);
		path.put("invoice", invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		node.put("reference", "123451234567");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testWithoutExistingInvoiceToAppendTo() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testMissingCreditor() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", output);
		path.put("invoice", invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("invoice", "12345");
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("reference", "3139471430009017");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", "1234567");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode resultNode = mapper.readTree(result.toString());
		assertEquals(ArrayNode.class, resultNode.getClass());
		Iterator<Entry<String, JsonNode>> entries = resultNode.fields();
		while (entries.hasNext())
		{
			Entry<String, JsonNode> next = entries.next();
			if (next.getKey().equals("creditor.name"))
			{
				assertEquals("'creditor.name' muss den Namen des Rechnungstellers enthalten (maximal 70 Bustaben).", next.getValue());
			}
			else if (next.getKey().equals("creditor.address"))
			{
				assertEquals("'creditor.address' muss die Adresse des Rechnungstellers enthalten (maximal 70 Bustaben).", next.getValue());
			}
			else if (next.getKey().equals("creditor.city"))
			{
				assertEquals("'creditor.city' muss Postleitzahl und Ort des Rechnungstellers enthalten (maximal 70 Bustaben).", next.getValue());
			}
			else if (next.getKey().equals("creditor.country"))
			{
				assertEquals("'creditor.country' muss den zweistelligen Landcode gemäss ISO 3166 des Rechnungstellers enthalten.", next.getValue());
			}
		}
	}
	
	@Test
	public void testMissingDebtor() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", output);
		path.put("invoice", invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("invoice", "12345");
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("reference", "3139471430009017");
		node.put("message", "Abonnement für 2020");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result.toString());
	}
	
	@Test
	public void testWithoutIban() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", output);
		path.put("invoice", invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode resultNode = mapper.readTree(result.toString());
		assertEquals(ArrayNode.class, resultNode.getClass());
		assertEquals(1, resultNode.size());
		Iterator<Entry<String, JsonNode>> entries = resultNode.fields();
		while (entries.hasNext())
		{
			Entry<String, JsonNode> next = entries.next();
			assertEquals("iban", next.getKey());
			assertEquals("'iban' muss die QRIban des Rechnungstellers enthalten.", next.getValue());
		}
	}

	@Test
	public void testDocumentToAppendToDoesNotExist() throws JsonMappingException, JsonProcessingException
	{
		String nonExistentPath = (System.getProperty("user.home") + File.separator + UUID.randomUUID() + ".pdf").replace('\\', '/');
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", output);
		path.put("invoice", nonExistentPath);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode resultNode = mapper.readTree(result.toString());
		assertEquals(ArrayNode.class, resultNode.getClass());
		assertEquals(1, resultNode.size());
		Iterator<Entry<String, JsonNode>> entries = resultNode.fields();
		while (entries.hasNext())
		{
			Entry<String, JsonNode> next = entries.next();
			assertEquals("Quelldatei", next.getKey());
			assertEquals("Die Quelldatei '" + nonExistentPath + "' existiert nicht. Sie muss für die Verarbeitung vorhanden sein", next.getValue());
		}
	}
	
	@Test
	public void testWithBillAsPng() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", output);
		path.put("invoice", invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testWithBillAsPngAlone() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", output);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testWithoutAmount() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", output);
		path.put("invoice", invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testWith0Amount() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", output);
		path.put("invoice", invoice);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_EXTRA_SPACE.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 0);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result);	
	}
	
	@Test
	public void testQRBillOnly() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			path.put("output", "C:\\Users\\christian\\QRBill.pdf");
		}
		else
		{
			path.put("output", "/Festplatte/Users/christian/QRBill.pdf");
		}
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		File file = null;
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			file = new File(path.get("output").asText());
		}
		if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
		{
			file = new File("/Volumes" + path.get("output").asText());
		}
		assertEquals("OK", result);	
		assertTrue(file.exists());
		file.delete();
	}
	
	@Test
	public void testQRBillWithInvalidPath() throws JsonMappingException, JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			path.put("output", "/C:/Users/christian/QRBill.pdf");
		}
		else
		{
			path.put("output", "/Test/Users/christian/QRBill.pdf");
		}
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PDF.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode resultNode = mapper.readTree(result.toString());
		assertEquals(ArrayNode.class, resultNode.getClass());
		assertEquals(1, resultNode.size());
		Iterator<Entry<String, JsonNode>> entries = resultNode.fields();
		while (entries.hasNext())
		{
			Entry<String, JsonNode> next = entries.next();
			assertEquals("path.output", next.getKey());
			assertEquals("Der Pfad für die generierte Daten muss gültig sein (Systempfad oder URI).", next.getValue());
		}
	}
	
	@Test
	public void testQRBillWithFilePath() throws JsonMappingException, JsonProcessingException
	{
		String out = null;
		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
		{
			out = "C:/Users/christian/Documents/QRBill.pdf";
		}
		if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
		{
			out = "/Festplatte/Users/christian/Documents/QRBill.pdf";
		}
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", out);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		assertEquals("OK", result.toString());	
	}
	
	@Test
	public void testQRBillWithMissingCurrency() throws JsonMappingException, JsonProcessingException
	{
		String out = (System.getProperty("java.io.tmpdir") + UUID.randomUUID() + ".png");
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		ObjectNode path = node.putObject("path");
		path.put("output", out);
		ObjectNode form = node.putObject("form");
		form.put("output_size", OutputSize.QR_BILL_ONLY.name());
		form.put("graphics_format", GraphicsFormat.PNG.name());
		form.put("language", Language.DE.name());
		node.put("iban", "CH4431999123000889012");
		node.put("amount", 199.95);
//		node.put("currency", "CHF");
		node.put("invoice", 10456);
		ObjectNode creditor = node.putObject("creditor");
		creditor.put("name", "Robert Schneider AG");
		creditor.put("address", "Rue du Lac 1268/2/22");
		creditor.put("city", "2501 Biel");
		creditor.put("country", "CH");
		node.put("message", "Abonnement für 2020");
		ObjectNode debtor = node.putObject("debtor");
		debtor.put("number", 9048);
		debtor.put("name", "Pia-Maria Rutschmann-Schnyder");
		debtor.put("address", "Grosse Marktgasse 28");
		debtor.put("city", "9400 Rorschach");
		debtor.put("country", "CH");
		Object result = new SwissQRBillGenerator().generate(node.toString());
		JsonNode resultNode = mapper.readTree(result.toString());
		assertEquals(ArrayNode.class, resultNode.getClass());
		assertEquals(1, resultNode.size());
		Iterator<Entry<String, JsonNode>> entries = resultNode.fields();
		while (entries.hasNext())
		{
			Entry<String, JsonNode> next = entries.next();
			assertEquals("currency", next.getKey());
			assertEquals("'currency' muss eine gültige Währung im ISO 4217 Format (3 Buchstaben) sein.", next.getValue());
		}
	}
	
}
