package com.alexzh.coffeedrinks.api

import com.alexzh.coffeedrinks.api.api.mapper.CoffeeDrinkMapper
import com.alexzh.coffeedrinks.api.api.model.CoffeeDrinkWithFavourite
import com.alexzh.coffeedrinks.api.api.model.CoffeeDrinkWithoutFavourite
import com.alexzh.coffeedrinks.api.auth.JwtService
import com.alexzh.coffeedrinks.api.com.alexzh.coffeedrinks.api.addAuthHeader
import com.alexzh.coffeedrinks.api.com.alexzh.coffeedrinks.api.launchTestApp
import com.alexzh.coffeedrinks.api.com.alexzh.coffeedrinks.api.stubAuthVerifier
import com.alexzh.coffeedrinks.api.data.model.CoffeeDrink
import com.alexzh.coffeedrinks.api.data.model.User
import com.alexzh.coffeedrinks.api.data.repository.CoffeeDrinkRepository
import com.alexzh.coffeedrinks.api.data.repository.UserRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
class CoffeeDrinksApiTest {
    private val coffeeDrinkRepository = mockk<CoffeeDrinkRepository>()
    private val userRepository = mockk<UserRepository>()
    private val jwtService = mockk<JwtService>()

    private val coffeeDrinkMapper = CoffeeDrinkMapper()

    @Test
    fun `should return 2 coffee drinks when coffee-drinks request executed and no active user`() {
        // TODO: use generators for data
        val coffeeDrinks = listOf(
            CoffeeDrink(1L, "Coffee 1", "-", "Description 1", "Ingredients 1", true),
            CoffeeDrink(2L, "Coffee 2", "no", "Description 2", "Ingredients 2", true)
        )

        launchTestApp(
            coffeeDrinkRepository = coffeeDrinkRepository,
            coffeeDrinkMapper = coffeeDrinkMapper
        ) {
            stubGetCoffeeDrinks(coffeeDrinks)

            handleRequest(HttpMethod.Get, "/api/v1/coffee-drinks").apply {
                // TODO: use custom assertion
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                    coffeeDrinks.map { coffeeDrinkMapper.mapToCoffeeDrinkWithoutFavourite(it) },
                    Gson().fromJson(response.content, object : TypeToken<List<CoffeeDrinkWithoutFavourite>>() {}.type)
                )
            }
        }
    }

    @Test
    fun `should return 2 coffee drinks when coffee-drinks request executed with active user`() {
        // TODO: use generators for data
        val user = User(42L, "Test User", "test@test.com", "12345")
        val coffeeDrinks = listOf(
                CoffeeDrink(1L, "Coffee 1", "-", "Description 1", "Ingredients 1", true),
                CoffeeDrink(2L, "Coffee 2", "no", "Description 2", "Ingredients 2", true)
        )

        stubAuthVerifier(jwtService)

        launchTestApp(
                coffeeDrinkRepository = coffeeDrinkRepository,
                coffeeDrinkMapper = coffeeDrinkMapper,
                userRepository = userRepository,
                jwtService = jwtService
        ) {
            stubGetCoffeeDrinksById(user.id, coffeeDrinks)
            stubGetUserById(user.id, user)

            handleRequest(HttpMethod.Get, "/api/v1/coffee-drinks") {
                addAuthHeader(this@launchTestApp, user)
            }.apply {
                // TODO: use custom assertion
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                        coffeeDrinks.map { coffeeDrinkMapper.mapToCoffeeDrinkWithFavourite(it) },
                        Gson().fromJson(response.content, object : TypeToken<List<CoffeeDrinkWithFavourite>>() {}.type)
                )
            }
        }
    }

    @Test
    fun `should return empty list when database have no data executed`() {
        val coffeeDrinks = emptyList<CoffeeDrink>()

        launchTestApp(
            coffeeDrinkRepository = coffeeDrinkRepository,
            coffeeDrinkMapper = coffeeDrinkMapper
        ) {
            stubGetCoffeeDrinks(coffeeDrinks)

            handleRequest(HttpMethod.Get, "/api/v1/coffee-drinks").apply {
                // TODO: use custom assertion
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(coffeeDrinks, Gson().fromJson(response.content, object : TypeToken<List<CoffeeDrink>>() {}.type) )
            }
        }
    }

    @Test
    fun `should return coffee drink by id when coffee-drinks request with id = 42 is executed`() {
        // TODO: use generators for data
        val id = 42L
        val coffeeDrink = CoffeeDrink(1L, "Coffee 1", "-", "Description 1", "Ingredients 1", true)

        launchTestApp(
            coffeeDrinkRepository = coffeeDrinkRepository,
            coffeeDrinkMapper = coffeeDrinkMapper
        ) {
            stubGetCoffeeDrinkById(id, coffeeDrink)

            handleRequest(HttpMethod.Get, "/api/v1/coffee-drinks/$id").apply {
                // TODO: use custom assertion
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(coffeeDrink, Gson().fromJson(response.content, CoffeeDrink::class.java) )
            }
        }
    }

    @Test
    fun `should return response with 404 code when coffee-drinks request with id = -1 is executed`() {
        val id = -1L

        launchTestApp(
            coffeeDrinkRepository = coffeeDrinkRepository,
            coffeeDrinkMapper = coffeeDrinkMapper
        ) {
            stubGetCoffeeDrinkById(id, null)

            handleRequest(HttpMethod.Get, "/api/v1/coffee-drinks/$id").apply {
                // TODO: use custom assertion
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    private fun stubGetCoffeeDrinks(
        coffeeDrinks: List<CoffeeDrink>
    ) {
        coEvery { coffeeDrinkRepository.getCoffeeDrinks() } returns coffeeDrinks
    }

    private fun stubGetCoffeeDrinksById(
        userId: Long,
        coffeeDrinks: List<CoffeeDrink>
    ) {
        coEvery { coffeeDrinkRepository.getCoffeeDrinksByUser(userId) } returns coffeeDrinks
    }

    private fun stubGetUserById(
        userId: Long,
        user: User
    ) {
        coEvery { userRepository.getUserById(userId) } returns user
    }

    private fun stubGetCoffeeDrinkById(
        id: Long,
        coffeeDrink: CoffeeDrink? = null
    ) {
        coEvery { coffeeDrinkRepository.getCoffeeDrinkById(id) }.returns(coffeeDrink)
    }
}