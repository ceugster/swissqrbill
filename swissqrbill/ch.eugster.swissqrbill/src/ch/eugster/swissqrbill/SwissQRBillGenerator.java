package ch.eugster.swissqrbill;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.codecrete.qrbill.canvas.PDFCanvas;
import net.codecrete.qrbill.generator.Address;
import net.codecrete.qrbill.generator.Bill;
import net.codecrete.qrbill.generator.BillFormat;
import net.codecrete.qrbill.generator.GraphicsFormat;
import net.codecrete.qrbill.generator.Language;
import net.codecrete.qrbill.generator.OutputSize;
import net.codecrete.qrbill.generator.QRBill;
import net.codecrete.qrbill.generator.ValidationResult;

public class SwissQRBillGenerator 
{
	public Object generate(String json) 
	{
		// convert JSON string to Map
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode result = mapper.createArrayNode();
		JsonNode node = null;
		try 
		{
			node = mapper.readTree(json);
			if (node == null)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("Parameter", "Der übergebene Parameter konnte nicht gelesen werden. Handelt es sich um ein Json Objekt?");
				result.add(msg);
			}
		} 
		catch (IllegalArgumentException e) 
		{
			ObjectNode msg = mapper.createObjectNode();
			msg.put("Parameter", "Der übergebene Parameter enthält ein ungültiges Element (" + e.getLocalizedMessage() + ").");
			result.add(msg);
		} 
		catch (JsonMappingException e) 
		{
			ObjectNode msg = mapper.createObjectNode();
			msg.put("Parameter", "Der übergebene Parameter konnte nicht als JSON Object aufgebaut werden (" + e.getLocalizedMessage() + "). Handelt es sich um ein gültiges Json Objekt?");
			result.add(msg);
		} 
		catch (JsonProcessingException e) 
		{
			ObjectNode msg= mapper.createObjectNode();
			msg.put("Parameter", "Der übergebene Parameter konnte nicht verarbeitet werden (" + e.getLocalizedMessage() + "). Handelt es sich um ein Json Objekt?");
			result.add(msg);
		}

		if (node != null)
		{
			String path = node.get("path").get("output").asText();
			if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
			{
				File file = new File(path);
				if (file.isAbsolute() && !file.getAbsolutePath().startsWith("/Volumes"))
				{
					path = "/Volumes" + path;
				}
			}
			Path output = null;
			try
			{
				URI uri = new URI(path);
				output = Paths.get(uri);
			}
			catch (URISyntaxException e)
			{
				output = Paths.get(path);
			}
			catch (FileSystemNotFoundException e)
			{
				output = Paths.get(path);
			}
			catch (IllegalArgumentException e)
			{
				output = Paths.get(path);
			}
			catch (Exception e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("path.output", "Der Pfad für die generierte Daten muss gültig sein (Systempfad oder URI).");
				result.add(msg);
			}

			BillFormat format = new BillFormat();
			format.setLanguage(guessLanguage(node.get("form")));
			try
			{
				format.setFontFamily("Arial");
				format.setGraphicsFormat(selectGraphicsFormat(node.get("form")));
			}
			catch (IllegalArgumentException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("form.graphics_format", buildGraphicsFormatErrorMessage());
				result.add(msg);
			}
			try
			{
				format.setOutputSize(selectOutputSize(node.get("form")));
			}
			catch (IllegalArgumentException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("form.output_size", buildOutputSizeErrorMessage());
				result.add(msg);
			}

			// Setup bill
			Bill bill = new Bill();
			bill.setFormat(format);
			if (node.get("amount") != null && node.get("amount").asDouble() > 0D)
			{
				bill.setAmountFromDouble(Double.valueOf(node.get("amount").asDouble()));
			}
			try
			{
				bill.setCurrency(node.get("currency").asText());
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("currency", "'currency' muss eine gültige Währung im ISO 4217 Format (3 Buchstaben) sein.");
				result.add(msg);
			}
			bill.setReferenceType(Bill.REFERENCE_TYPE_NO_REF);
	
			// Set creditor
			Address creditor = new Address();
			try
			{
				creditor.setName(node.get("creditor").get("name").asText());
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("creditor.name", "'creditor.name' muss den Namen des Rechnungstellers enthalten (maximal 70 Buchstaben).");
				result.add(msg);
			}
			try
			{
				creditor.setAddressLine1(node.get("creditor").get("address").asText());
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("creditor.address", "'creditor.address' muss die Adresse des Rechnungstellers enthalten (maximal 70 Buchstaben).");
				result.add(msg);
			}
			try
			{
				creditor.setAddressLine2(node.get("creditor").get("city").asText());
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("creditor.city", "'creditor.city' muss Postleitzahl und Ort des Rechnungstellers enthalten (maximal 70 Buchstaben).");
				result.add(msg);
			}
			try
			{
				creditor.setCountryCode(node.get("creditor").get("country").asText());
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("creditor.country", "'creditor.country' muss den zweistelligen Landcode gemäss ISO 3166 des Rechnungstellers enthalten.");
				result.add(msg);
			}
			bill.setCreditor(creditor);
			try
			{
				bill.setAccount(node.get("iban").asText());
			}
			catch (NullPointerException e)
			{
				ObjectNode msg = mapper.createObjectNode();
				msg.put("iban", "'iban' muss die QRIban des Rechnungstellers enthalten.");
				result.add(msg);
			}
	
			// more bill data
			StringBuilder reference = new StringBuilder();
			if (Objects.isNull(node.get("reference")))
			{
				if (node.get("invoice") != null)
				{
					try
					{
						reference = reference.append(new BigInteger(node.get("invoice").asText()).toString());
					}
					catch (NumberFormatException e)
					{
						// Do nothing
					}
				}
				if (node.get("debtor") != null && node.get("debtor").get("number") != null)
				{
					try
					{
						reference = reference.append(new BigInteger(node.get("debtor").get("number").asText()).toString());
					}
					catch (NumberFormatException e)
					{
						// Do nothing
					}
				}
			}
			else
			{
				try
				{
					reference = reference.append(new BigInteger(node.get("reference").asText()).toString());
				}
				catch (NumberFormatException e)
				{
					// Do nothing: reference is already initialized with ""
				}
			}
			if (reference.length() == 27)
			{
				bill.setReference(reference.toString());
			}
			else
			{
				bill.createAndSetQRReference(reference.toString());
			}
			bill.setUnstructuredMessage(node.get("message").asText());
			
			// Set creditor
			if (node.get("debtor") != null)
			{
				Address debtor = new Address();
				try
				{
					debtor.setName(node.get("debtor").get("name").asText());
				}
				catch (NullPointerException e)
				{
					ObjectNode msg = mapper.createObjectNode();
					msg.put("debtor.name", "'debtor.name' muss den Namen des Rechnungempfängers enthalten (maximal 70 Buchstaben).");
					result.add(msg);
				}
				try
				{
					debtor.setAddressLine1(node.get("debtor").get("address").asText());
				}
				catch (NullPointerException e)
				{
					ObjectNode msg = mapper.createObjectNode();
					msg.put("debtor.address", "'debtor.address' muss die Adresse des Rechnungsempfängers enthalten (maximal 70 Buchstaben).");
					result.add(msg);
				}
				try
				{
					debtor.setAddressLine2(node.get("debtor").get("city").asText());
				}
				catch (NullPointerException e)
				{
					ObjectNode msg = mapper.createObjectNode();
					msg.put("debtor.city", "'debtor.city' muss Postleitzahl und Ort des Rechnungsempfängers enthalten (maximal 70 Buchstaben).");
					result.add(msg);
				}
				try
				{
					debtor.setCountryCode(node.get("debtor").get("country").asText());
				}
				catch (NullPointerException e)
				{
					ObjectNode msg = mapper.createObjectNode();
					msg.put("debtor.country", "'debtor.country' muss den zweistelligen Landcode gemäss ISO 3166 des Rechnungsempfängers enthalten.");
					result.add(msg);
				}
			}
	
			// Validate QR bill
			ValidationResult validation = QRBill.validate(bill);
			if (validation.isValid() && result.isEmpty())
			{
				Path invoice = null;
				if (node.get("path").get("invoice") != null && node.get("path").get("invoice").asText() != null)
				{
					try
					{
						URI uri = new URI(node.get("path").get("invoice").asText());
						invoice = Paths.get(uri);
					}
					catch (URISyntaxException e)
					{
						invoice = Paths.get(node.get("path").get("invoice").asText());
					}
					catch (FileSystemNotFoundException e)
					{
						invoice = Paths.get(node.get("path").get("invoice").asText());
					}
					catch (IllegalArgumentException e)
					{
						invoice = Paths.get(node.get("path").get("invoice").asText());
					}
					catch (Exception e)
					{
						ObjectNode msg = mapper.createObjectNode();
						msg.put("Quelldatei", "Falls die QRBill an ein bestehendes Dokument angefügt werden soll, so muss dieses Dokument bereits bestehen und einen gültigen Namen haben.");
						result.add(msg);
					}
				}
				
				if (!Objects.isNull(invoice))
				{
					if (invoice.toFile().exists())
					{
						PDFCanvas canvas = null;
						try
						{
							byte[] targetArray = null;
							InputStream is = null;
							try
							{
								is = new FileInputStream(invoice.toFile());
								targetArray = new byte[is.available()];
								is.read(targetArray);
							}
							finally
							{
								if (is != null)
								{
									is.close();
								}
							}

							canvas = new PDFCanvas(targetArray, PDFCanvas.LAST_PAGE);
							QRBill.draw(bill, canvas);
							return "OK";
						}
						catch (IOException e)
						{
							ObjectNode msg = mapper.createObjectNode();
							msg.put("Ausgabedatei", "Das Dokument, an das die QRBill angehängt werden soll, konnte nicht geöffnet werden.");
							result.add(msg);
						}
						finally
						{
							if (canvas != null)
							{
								try
								{
									canvas.saveAs(output);
									canvas.close();
								}
								catch (IOException e)
								{
									ObjectNode msg = mapper.createObjectNode();
									msg.put("Zieldatei", "Die Zieldatei '" + invoice.toString() + "' kann nicht gespeichert werden.");
									result.add(msg);
								}
							}
						}
					}
					else
					{
						ObjectNode msg = mapper.createObjectNode();
						msg.put("Quelldatei", "Die Quelldatei '" + invoice.toFile().getAbsolutePath() + "' existiert nicht. Sie muss für die Verarbeitung vorhanden sein.");
						result.add(msg);
					}
				}
				else
				{
					// Generate QR bill
					byte[] bytes = QRBill.generate(bill);
					try 
					{
						if (output.toFile().exists())
						{
							output.toFile().delete();
						}
						output.toFile().createNewFile();
						OutputStream os = null;
						try
						{
							os = new FileOutputStream(output.toFile());
							os.write(bytes);
						}
						finally
						{
							if (os != null)
							{
								os.flush();
								os.close();
							}
						}
						return "OK";
					} 
					catch (FileNotFoundException e) 
					{
						ObjectNode msg = mapper.createObjectNode();
						msg.put("Zieldatei", "Die Zieldatei kann nicht gefunden werden.");
						result.add(msg);
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
						ObjectNode msg = mapper.createObjectNode();
						msg.put("Zieldatei", "Beim Zugriff auf die Zieldatei '" + output.toString() + "' ist ein Fehler aufgetreten.");
						result.add(msg);
					} 
				}
			}
		}
		return result.toString();
	}
	
	private Language guessLanguage(JsonNode node)
	{
		JsonNode languageNode = node.get("language");
		if (languageNode != null && languageNode.asText() != null && !languageNode.asText().trim().isEmpty())
		{
			String[] availableLanguages = new String[] { "EN", "DE", "FR", "IT" };
			for (String availableLanguage : availableLanguages)
			{
				if (availableLanguage.equals(languageNode.asText()))
				{
					return Language.valueOf(availableLanguage);
				}
			}
		}
		if (Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage()))
		{
			return Language.DE;
		}
		else if (Locale.getDefault().getLanguage().equals(Locale.FRENCH.getLanguage()))
		{
			return Language.FR;
		}
		else if (Locale.getDefault().getLanguage().equals(Locale.ITALIAN.getLanguage()))
		{
			return Language.IT;
		}
		else
		{
			return Language.EN;
		}
	}
	
	private OutputSize selectOutputSize(JsonNode node) throws IllegalArgumentException
	{
		OutputSize outputSize = null;
		JsonNode size = node.get("output_size");
		if (size == null || size.asText() == null || size.asText().trim().isEmpty())
		{
			if (node.get("path").get("invoice") == null)
			{
				outputSize = OutputSize.A4_PORTRAIT_SHEET;
			}
			else
			{
				outputSize = OutputSize.QR_BILL_EXTRA_SPACE;
			}
		}
		try
		{
			outputSize = OutputSize.valueOf(size.asText());
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException(buildOutputSizeErrorMessage());
		}
		return outputSize;
	}

	private GraphicsFormat selectGraphicsFormat(JsonNode node) throws IllegalArgumentException
	{
		JsonNode graphicsFormat = node.get("graphics_format");
		if (graphicsFormat == null || graphicsFormat.asText() == null || graphicsFormat.asText().trim().isEmpty())
		{
			throw new IllegalArgumentException(buildGraphicsFormatErrorMessage());
		}
		GraphicsFormat format = null;
		try
		{
			format = GraphicsFormat.valueOf(graphicsFormat.asText());
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException(buildGraphicsFormatErrorMessage());
		}
		if (format == null || format.getClass().isAnnotationPresent(Deprecated.class))
		{
			throw new IllegalArgumentException(buildGraphicsFormatErrorMessage());
		}
		return format;
	}
	
	private String buildOutputSizeErrorMessage()
	{
		StringBuilder builder = new StringBuilder();
		for (OutputSize size : OutputSize.values())
		{
			builder = builder.append(size.name() + ", ");
		}
		String values = builder.toString().trim();
		values = values.substring(0, values.length() - 1);
		return "'output_size' muss eines der folgenden Werte sein: " + values;
	}
	
	private String buildGraphicsFormatErrorMessage()
	{
		StringBuilder builder = new StringBuilder();
		for (GraphicsFormat format : GraphicsFormat.values())
		{
			builder = builder.append(format.name() + ", ");
		}
		String values = builder.toString().trim();
		values = values.substring(0, values.length() - 1);
		return "'graphics_format' muss eines der folgenden Werte sein: " + values;
	}
	
}