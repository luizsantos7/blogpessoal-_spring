package com.generation.blogpessoal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.generation.blogpessoal.model.Usuario;
import com.generation.blogpessoal.model.UsuarioLogin;
import com.generation.blogpessoal.repository.UsuarioRepository;
import com.generation.blogpessoal.service.UsuarioService;
import com.generation.blogpessoal.util.TestBuilder;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UsuarioControllerTest {
	
	@Autowired
	private TestRestTemplate testRestTemplate;
	
	@Autowired
	private UsuarioService usuarioService;
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	
	private static final String USUARIO_ROOT_EMAIL = "root@email.com";
	private static final String USUARIO_ROOT_SENHA = "rootroot";
	private static final String BASE_URL_USUARIOS = "/usuarios";
	
	@BeforeAll
	void start() {
		usuarioRepository.deleteAll();
		usuarioService.cadastrarUsuario(TestBuilder.criarUsuarioRoot());
	}
	
	@Test
	@DisplayName("Deve cadastrar um novo usuário com sucesso")
	public void deveCadastrarUsuario() {
		
		//Given
		Usuario usuario = TestBuilder.criarUsuario(null, "Renata Negrini", "renata_negrini@email.com.br", "12345678");
		
		// When
		HttpEntity<Usuario> requisicao = new HttpEntity<Usuario>(usuario);
		ResponseEntity<Usuario> resposta = testRestTemplate.exchange(
				BASE_URL_USUARIOS + "/cadastrar", HttpMethod.POST, requisicao, Usuario.class);
		
		// Then
		assertEquals (HttpStatus.CREATED, resposta.getStatusCode());
		assertEquals("Renata Negrini", resposta.getBody().getNome());
		assertEquals("renata_negrini@email.com.br", resposta.getBody().getUsuario());
	}
	
	@Test
	@DisplayName("não deve permitir a duplicação do usuario")
	public void naoDeveDuplicarUsuario() {
		
		//Given
		Usuario usuario = TestBuilder.criarUsuario(null, "Angelo dos Santos", "angelo@email.com.br", "12345678");
		usuarioService.cadastrarUsuario(usuario);
		
		//When
		HttpEntity<Usuario> requisicao = new HttpEntity<Usuario>(usuario);
		ResponseEntity<Usuario> resposta = testRestTemplate.exchange(
				BASE_URL_USUARIOS + "/cadastrar", HttpMethod.POST, requisicao, Usuario.class);
	
		//Then
		assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
	}
	
	@Test
	@DisplayName("Deve atualizar um usuário existente")
	public void deveAtualizarUmUsuario() {
		
		//Given
		Usuario usuario = TestBuilder.criarUsuario(null, "Juliana Andrews", "juliana_andrews@email.com.br",
				"juliana123");
		Optional<Usuario> usuarioCadastrado = usuarioService.cadastrarUsuario(usuario);
		
		Usuario usuarioUpdate = TestBuilder.criarUsuario(usuarioCadastrado.get().getId(), "Juliana Ramos", 
				"juliana_ramos@email.com.br", "juliana123");

		//When
		HttpEntity<Usuario> requisicao = new HttpEntity<>(usuarioUpdate);

		ResponseEntity<Usuario> resposta = testRestTemplate
				.withBasicAuth(USUARIO_ROOT_EMAIL, USUARIO_ROOT_SENHA)
				.exchange(BASE_URL_USUARIOS + "/atualizar", HttpMethod.PUT, requisicao, Usuario.class);

		//Then
		assertEquals(HttpStatus.OK, resposta.getStatusCode());
		assertEquals("Juliana Ramos", resposta.getBody().getNome());
		assertEquals("juliana_ramos@email.com.br", resposta.getBody().getUsuario());
	}
	

	@Test
	@DisplayName("Deve buscar usuário por ID com sucesso")
	public void deveBuscarUsuarioPorIdComSucesso() {
		
		Optional<Usuario> usuarioRoot = usuarioRepository.findByUsuario(USUARIO_ROOT_EMAIL);
		assertTrue(usuarioRoot.isPresent(), "Usuário root não encontrado para o teste");
		Long idExistente = usuarioRoot.get().getId();
		
		ResponseEntity<Usuario> resposta = testRestTemplate
				.withBasicAuth(USUARIO_ROOT_EMAIL, USUARIO_ROOT_SENHA)
				.exchange(BASE_URL_USUARIOS + "/" + idExistente, HttpMethod.GET, null, Usuario.class);
		
		assertEquals(HttpStatus.OK, resposta.getStatusCode());
		assertNotNull(resposta.getBody());
		assertEquals(idExistente, resposta.getBody().getId());
		assertEquals(USUARIO_ROOT_EMAIL, resposta.getBody().getUsuario());
	}

	@Test
	@DisplayName("Deve retornar status NOT_FOUND ao buscar usuário por ID inexistente")
	public void deveRetornarNotFoundAoBuscarPorIdInexistente() {
		
		Long idInexistente = 999999999L; 
		ResponseEntity<Usuario> resposta = testRestTemplate
				.withBasicAuth(USUARIO_ROOT_EMAIL, USUARIO_ROOT_SENHA)
				.exchange(BASE_URL_USUARIOS + "/" + idInexistente, HttpMethod.GET, null, Usuario.class);
		
		assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode());
	}

	@Test
	@DisplayName("Deve autenticar usuário com sucesso")
	public void deveAutenticarUsuarioComSucesso() {
		
		UsuarioLogin usuarioLoginValido = new UsuarioLogin();
		usuarioLoginValido.setUsuario(USUARIO_ROOT_EMAIL);
		usuarioLoginValido.setSenha(USUARIO_ROOT_SENHA);
		
		HttpEntity<UsuarioLogin> requisicaoLogin = new HttpEntity<>(usuarioLoginValido);
		ResponseEntity<UsuarioLogin> respostaLogin = testRestTemplate.exchange(
				BASE_URL_USUARIOS + "/logar", HttpMethod.POST, requisicaoLogin, UsuarioLogin.class);
		
		assertEquals(HttpStatus.OK, respostaLogin.getStatusCode());
		assertNotNull(respostaLogin.getBody());
		assertNotNull(respostaLogin.getBody().getToken(), "O token não deve ser nulo após login bem-sucedido");
	}

	@Test
	@DisplayName("Não deve autenticar usuário com credenciais inválidas")
	public void naoDeveAutenticarComCredenciaisInvalidas() {
		
		UsuarioLogin usuarioLoginInvalido = new UsuarioLogin();
		usuarioLoginInvalido.setUsuario(USUARIO_ROOT_EMAIL);
		usuarioLoginInvalido.setSenha("senhaErrada123");
		
		HttpEntity<UsuarioLogin> requisicaoLogin = new HttpEntity<>(usuarioLoginInvalido);
		ResponseEntity<UsuarioLogin> respostaLogin = testRestTemplate.exchange(
				BASE_URL_USUARIOS + "/logar", HttpMethod.POST, requisicaoLogin, UsuarioLogin.class);
		
		assertEquals(HttpStatus.UNAUTHORIZED, respostaLogin.getStatusCode());	
	}

	@Test
	@DisplayName("Deve retornar Unauthorized ao acessar endpoint protegido sem autenticação")
	public void deveRetornarUnauthorizedAoAcessarEndpointProtegidoSemAutenticacao() {
		
		ResponseEntity<String> resposta = testRestTemplate
				.exchange(BASE_URL_USUARIOS + "/all", HttpMethod.GET, null, String.class);
		
		assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
	}
}


