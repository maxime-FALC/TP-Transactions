package banking;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.hsqldb.cmdline.SqlFile;
import org.hsqldb.cmdline.SqlToolError;
import sun.security.util.Debug;


public class BankingTest {
	private static DataSource myDataSource; // La source de données à utiliser
	private static Connection myConnection ;	
	private BankingDAO myDAO;
	
	@Before
	public void setUp() throws SQLException, IOException, SqlToolError {
		// On crée la connection vers la base de test "in memory"
		myDataSource = getDataSource();
		myConnection = myDataSource.getConnection();
		// On initialise la base avec le contenu d'un fichier de test
		String sqlFilePath = BankingTest.class.getResource("testdata.sql").getFile();
		SqlFile sqlFile = new SqlFile(new File(sqlFilePath));
		sqlFile.setConnection(myConnection);
		sqlFile.execute();
		sqlFile.closeReader();	
		// On crée l'objet à tester
		myDAO = new BankingDAO(myDataSource);
	}
	
	@After
	public void tearDown() throws SQLException {
		myConnection.close();		
		myDAO = null; // Pas vraiment utile
	}

	
	@Test
	public void findExistingCustomer() throws SQLException {
		float balance = myDAO.balanceForCustomer(0);
		// attention à la comparaison des nombres à virgule flottante !
		// Le dernier paramètre correspond à la marge d'erreur tolérable
		assertEquals("Balance incorrecte !", 100.0f, balance, 0.001f);
	}

	@Test
	public void successfulTransfer() throws Exception {
		float amount = 10.0f;
		int fromCustomer = 0; // Le client 0 dispose de 100€ dans le jeu de tests
		int toCustomer = 1;
		// On mémorise les balances dans les deux comptes avant la transaction
		float before0 = myDAO.balanceForCustomer(fromCustomer);
		float before1 = myDAO.balanceForCustomer(toCustomer);
		// On exécute la transaction, qui doit réussir
		myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
		// Les balances doivent avoir été mises à jour dans les 2 comptes
		assertEquals("Balance incorrecte !", before0 - amount, myDAO.balanceForCustomer(fromCustomer), 0.001f);
		assertEquals("Balance incorrecte !", before1 + amount, myDAO.balanceForCustomer(toCustomer), 0.001f);				
	}
	

	public static DataSource getDataSource() throws SQLException {
		org.hsqldb.jdbc.JDBCDataSource ds = new org.hsqldb.jdbc.JDBCDataSource();
		ds.setDatabase("jdbc:hsqldb:mem:testcase;shutdown=true");
		ds.setUser("sa");
		ds.setPassword("sa");
		return ds;
	}
        
        
        /**
         * Vérifie que le compte débité dans le cas d'une transaction bancaire 
         * ne puisse pas être négatif.
         * Réalise donc une transaction dont le montant est supérieur à celui
         * contenu dans le compte
         * @throws Exception si une erreur arrive dans la base de donnée
         */
        @Test
        public void testCompteNegatif() throws Exception {
            
            float amount = 101.0f; // somme supérieure au montant du client 0
            int fromCustomer = 0; // Le client 0 dispose de 100€ dans le jeu de tests
            int toCustomer = 1;
            
            // On mémorise les balances dans les deux comptes avant la transaction
            float before0 = myDAO.balanceForCustomer(fromCustomer);
            float before1 = myDAO.balanceForCustomer(toCustomer);
            
            try{
                // On exécute la transaction, qui doit échouer et renvoyer une erreur
                myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
                fail("La transaction qui met un compte en négatif "
                        + "a fonctionné !");
                
            } catch (Exception e){
                //normal
            }
            
            // Les balances ne doivent pas avoir changé de montant
            assertEquals("Balance incorrecte !", before0, myDAO.balanceForCustomer(fromCustomer), 0.001f);
            assertEquals("Balance incorrecte !", before1, myDAO.balanceForCustomer(toCustomer), 0.001f);
        }
        
        
        
        
        /**
         * Vérifie qu'une transaction ne puisse pas être réalisée sur un compte
         * qui n'existe pas
         * Réalise donc une transaction avec un client imaginaire
         * @throws Exception si une erreur arrive dans la base de donnée
         */
        @Test
        public void testCompteDebiteInexistant() throws Exception {
            
            float amount = 1.0f; // somme supérieure au montant du client 0
            int fromCustomer = 0; // Le client 0 dispose de 100€ dans le jeu de tests
            int toCustomer = 5; // Le client 5 n'existe pas
            
            // On mémorise la balance du compte débité avant l'opération
            float before0 = myDAO.balanceForCustomer(fromCustomer);
            
            try{
                // On exécute la transaction, qui doit échouer et renvoyer une erreur
                myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
                fail("La transaction avec un compte débité inexistant a fonctionné !");
                
            } catch (Exception e){
                //normal
            }
            
            // La balance du compte ne doit pas avoir changé
            assertEquals("Balance incorrecte !", before0,
                    myDAO.balanceForCustomer(fromCustomer), 0.001f);
        }
        
        
        
        /**
         * Vérifie qu'une transaction ne puisse pas être réalisée sur un compte
         * qui n'existe pas
         * Réalise donc une transaction avec un client imaginaire
         * @throws Exception si une erreur arrive dans la base de donnée
         */
        @Test
        public void testCompteCrediteInexistant() throws Exception {
            
            float amount = 1.0f; // somme supérieure au montant du client 0
            int fromCustomer = 5; // Le client 5 n'existe pas
            int toCustomer = 1; // Le client 1 ne doit rien recevoir
            
            // On mémorise la balance du compte crédité avant l'opération
            float before = myDAO.balanceForCustomer(toCustomer);
            
            try{
                // On exécute la transaction, qui doit échouer et renvoyer une erreur
                myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
                fail("La transaction avec un compte crédité inexistant a fonctionné !");
                
            } catch (Exception e){
                //normal
            }
            
            // La balance du compte ne doit pas avoir changé
            assertEquals("Balance incorrecte !", before,
                    myDAO.balanceForCustomer(toCustomer), 0.001f);
        }
}
