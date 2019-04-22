package de.sidion.books.client.contracts.pact;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import de.sidion.books.client.adapter.BookCatalogRestService;
import de.sidion.books.client.domain.Book;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class BookCatalogConsumerPactTest {

    private static final String ROLE_HEADER = "X-ROLE";
    private static final String CONTENT_HEADER = "Content-Type";
    private static final String ROLE_HEADER_VALUE = "unprivileged use";
    private static final String CONTENT_HEADER_VALUE = "application/json;charset=UTF-8";

    @Rule
    public PactProviderRuleMk2 mockProvider =
            new PactProviderRuleMk2("book-catalog-service", "localhost", 9999, this);

    @Pact(consumer="books-client-catalog-rest-consumer")
    public RequestResponsePact pact(PactDslWithProvider builder) throws Exception {


        DslPart dslPart = new PactDslJsonArray()
                .object()
                .integerType("id")
                .stringMatcher("authorFirstName", ".*", "John")
                .stringMatcher("authorLastName", ".*", "Doe")
                .stringMatcher("title", ".*", "A book")
                .stringMatcher("isbn", "[0-9]{3}-[0-9]{1}-[0-9]{5}-[0-9]{3}-[0-9]{1}", "978-3-86680-192-9");

        return builder

                .given("get")
                .uponReceiving("A successful Api GET call")
                .path("/books")
                .headers(expectedRequestHeaders())
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(dslPart)
                .headers(responseHeaders())

                .toPact();

    }

    private Map<String, String> responseHeaders() {
        Map<String, String> map = new HashMap<>();
        map.put("Content-Type", "application/json;charset=UTF-8");
        return map;
    }

    private Map<String, String> expectedRequestHeaders() {
        Map<String, String> map = new HashMap<>();
        map.put(CONTENT_HEADER, CONTENT_HEADER_VALUE);
        map.put(ROLE_HEADER, ROLE_HEADER_VALUE);
        return map;
    }

    @Test
    @PactVerification
    public void verifyPact() {

        //we check the service call to the external system:
        RestTemplate template = new RestTemplate();
        BookCatalogRestService service = new BookCatalogRestService(template);

        List<Book> books = service.getAllBooks();

        //and then check that the mock response returns the expected object
        assertThat(books, Matchers.notNullValue());
        assertThat(books.size(), Matchers.greaterThanOrEqualTo(1));
        Book book = books.get(0);
        assertThat(book.getId(), Matchers.isA(String.class));
        assertThat(book.getAuthorFirstName(), Matchers.isA(String.class));
        assertThat(book.getAuthorLastName(), Matchers.isA(String.class));
        assertThat(book.getTitle(), Matchers.isA(String.class));
        assertThat(book.getIsbn().length(), Matchers.is(17));

    }


}