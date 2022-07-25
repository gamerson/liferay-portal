package com.liferay.couponpdf.service;

import com.liferay.couponpdf.service.dto.Coupon;
import com.liferay.couponpdf.service.dto.Coupons;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RequestMapping(value = "/coupons")
@RestController
public class CouponsController {

  @Qualifier("lcpDomain")
  @Resource
  private String _lcpDomain;

  @Autowired private WebClient _webClient;

  @PostMapping(
      produces = MediaType.APPLICATION_PDF_VALUE,
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      value = "/print")
  public ResponseEntity<StreamingResponseBody> printCoupon(
      @RequestParam(name = "couponId") String couponId, HttpServletResponse response) {

    _logger.debug("couponId = " + couponId);

    if (couponId != null && couponId.length() != 0) {
      return _printCouponById(couponId, response);
    } else {
      return _printNextCoupon(response);
    }
  }

  private ResponseEntity<StreamingResponseBody> _printNextCoupon(HttpServletResponse response) {
    _logger.info("Printing next available coupon");

    Coupons coupons =
        _webClient
            .get()
            .uri("https://" + _lcpDomain + "/o/c/coupons/")
            .retrieve()
            .bodyToMono(Coupons.class)
            .block();

    List<Coupon> availableCoupons =
        Arrays.stream(coupons.items).filter(coupon -> !coupon.issued).collect(Collectors.toList());

    _logger.info("Found " + availableCoupons.size() + " available coupons.");

    Optional<Coupon> issuedCoupon =
        availableCoupons.stream()
            .map(
                coupon -> {
                  coupon.issued = true;

                  Coupon patchedCoupon = _patchCoupon(coupon);

                  if (patchedCoupon != null && patchedCoupon.issued) {
                    return patchedCoupon;
                  }

                  return null;
                })
            .filter(Objects::nonNull)
            .findFirst();

    if (issuedCoupon.isPresent()) {
      _logger.info("Issuing coupon " + issuedCoupon.get().id);

      return new ResponseEntity<>(_streamPDFDocument(response, issuedCoupon.get()), HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private ResponseEntity<StreamingResponseBody> _printCouponById(
      String id, HttpServletResponse response) {

    _logger.info("Print couponId = " + id);

    // grab coupon object

    Coupon coupon =
        _webClient
            .get()
            .uri("https://" + _lcpDomain + "/o/c/coupons/" + id)
            .retrieve()
            .bodyToMono(Coupon.class)
            .block();

    // if coupon has not been found or it has already been issued
    // we can't print it

    if (coupon == null || coupon.issued) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    _logger.info("Issuing coupon " + coupon.code);

    coupon.issued = true;

    // patch coupon will fail if it has already been patched by another client

    Coupon patchedCoupon = _patchCoupon(coupon);

    if (patchedCoupon.issued) {
      _logger.info("Patched coupon.");

      return new ResponseEntity<>(_streamPDFDocument(response, patchedCoupon), HttpStatus.OK);
    }

    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  private Coupon _patchCoupon(Coupon coupon) {
    return _webClient
        .patch()
        .uri("https://" + _lcpDomain + "/o/c/coupons/" + coupon.id)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(coupon))
        .retrieve()
        .bodyToMono(Coupon.class)
        .block();
  }

  private StreamingResponseBody _streamPDFDocument(HttpServletResponse response, Coupon coupon) {
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "inline; filename=coupon.pdf");

    return out -> {
      Document document = new Document();

      final PdfWriter pdfWriter = PdfWriter.getInstance(document, response.getOutputStream());

      document.open();

      pdfWriter.getInfo().put(PdfName.CREATOR, new PdfString(Document.getVersion()));

      document.add(new Paragraph("Code: " + coupon.code));
      document.add(
          new Paragraph(
              "Issue Date: "
                  + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE).toString()));

      PdfContentByte pdfContent = pdfWriter.getDirectContent();

      pdfContent.setLineWidth(3F);
      pdfContent.rectangle(20F, 735F, 185F, 90F);
      pdfContent.stroke();
      pdfContent.sanityCheck();

      document.close();
    };
  }

  private static final Logger _logger = LoggerFactory.getLogger(CouponsController.class);
}
