package br.com.zup.edu.carros

import br.com.zup.edu.CarroRequest
import br.com.zup.edu.CarrosGrpcServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CarrosEndpointTest(
        val repository: CarroRepository,
        val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub
) {

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve adicionar um novo carro`() {
        // ação
        val request = CarroRequest.newBuilder()
                .setModelo("Fiat Uno")
                .setPlaca("AUQ-7564")
                .build()
        val response = grpcClient.adicionar(request)

        with(response){
            assertNotNull(id)
            assertTrue(repository.existsById(id))
        }
    }

    @Test
    fun `não deve adicionar novo carro quando placa já existe`() {
        // cenário
        val carroExistente = repository.save(Carro("Palio", "OIP-9876"))

        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(CarroRequest.newBuilder()
                    .setModelo("Ferrari")
                    .setPlaca(carroExistente.placa)
                    .build())
        }

        // validação
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("carro com placa existente", status.description)
        }
    }

    @Test
    fun `não deve adicionar novo carro quando dados são inválidos`() {
        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(CarroRequest.newBuilder()
                    .setModelo("")
                    .setPlaca("")
                    .build())
        }

        // validação
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos", status.description)
        }
    }
}

@Factory
class Clients {
    @Singleton
    fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
            CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub {
        return CarrosGrpcServiceGrpc.newBlockingStub(channel)
    }
}