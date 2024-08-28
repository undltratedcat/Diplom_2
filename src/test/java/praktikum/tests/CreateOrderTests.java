package praktikum.tests;

import io.qameta.allure.Link;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import praktikum.entities.request.Ingredient;
import praktikum.entities.response.IngredientsResponse;
import praktikum.handlers.apiclients.OrderApiClient;
import praktikum.handlers.apiclients.ResponseChecks;
import praktikum.handlers.apiclients.UserApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.fail;

@DisplayName("4. Создание заказа")
@Link(value = "Документация", url = "https://code.s3.yandex.net/qa-automation-engineer/java/cheatsheets/paid-track/diplom/api-documentation.pdf")
public class CreateOrderTests {

    private String token; // Оставляем на уровне класса, так как используется в нескольких методах
    private List<Ingredient> ingredients = new ArrayList<>(); // Оставляем на уровне класса, так как используется в нескольких тестах
    private final OrderApiClient orderApi = new OrderApiClient();
    private final UserApiClient userApi = new UserApiClient();
    private final ResponseChecks checks = new ResponseChecks();

    @Before
    @Step("Подготовка тестовых данных")
    public void prepareTestData() {
        // Перемещаем email, password, name в локальные переменные
        String email = "e-mail_" + UUID.randomUUID() + "@mail.com";
        String password = "pass_" + UUID.randomUUID();
        String name = "name";

        // Создание пользователя
        Response response = userApi.createUser(email, password, name);
        checks.checkStatusCode(response, 200);

        // Получение токена
        if (response.getStatusCode() == 200) {
            token = userApi.getToken(response);
        }

        // Получение списка ингредиентов
        response = orderApi.getIngredientList();
        checks.checkStatusCode(response, 200);

        ingredients = response.body().as(IngredientsResponse.class).getData();

        if(token == null || ingredients.isEmpty())
            fail("Отсутствует токен или не получен список ингредиентов");
    }

    @After
    @Step("Удаление тестовых пользователей")
    public void cleanTestData() {
        if(token == null)
            return;

        checks.checkStatusCode(userApi.deleteUser(token), 202);
    }

    @Test
    @DisplayName("Создание заказа: с авторизацией и с ингредиентами")
    public void createOrderWithAuthAndIngredientsIsSuccess() {
        Response response = orderApi.createOrder(
                List.of(ingredients.get(0).get_id(), ingredients.get(ingredients.size() - 1).get_id()),
                token
        );

        checks.checkStatusCode(response, 200);
        checks.checkLabelSuccess(response, "true");
    }

    @Test
    @DisplayName("Создание заказа: без авторизации и с ингредиентами")
    public void createOrderWithoutAuthAndWithIngredientsIsFailed() {
        Response response = orderApi.createOrder(
                List.of(ingredients.get(0).get_id(), ingredients.get(ingredients.size() - 1).get_id()),"Wrong_token");

        checks.checkStatusCode(response, 403);
    }

    @Test
    @DisplayName("Создание заказа: с авторизацией и без ингредиентов")
    public void createOrderWithAuthAndWithoutIngredientsIsSuccess() {
        Response response = orderApi.createOrder(
                List.of(),
                token
        );

        checks.checkStatusCode(response, 400);
        checks.checkLabelSuccess(response, "false");
        checks.checkLabelMessage(response, "Ingredient ids must be provided");
    }

    @Test
    @DisplayName("Создание заказа: с неверным хешем ингредиентов")
    public void createOrderWithAuthAndIncorrectIngredientsIsFailed() {
        Response response = orderApi.createOrder(
                List.of(ingredients.get(0).get_id(), UUID.randomUUID().toString()),
                token
        );

        checks.checkStatusCode(response, 500);
    }
}
