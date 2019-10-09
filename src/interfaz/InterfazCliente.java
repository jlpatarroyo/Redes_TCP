package interfaz;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import mundo.Cliente;

public class InterfazCliente extends JFrame implements ActionListener
{
	private JButton botonCrear;
	private final static String CREAR = "Crear";
	private Cliente cliente;
	private static final String IP = "localhost";
	
	public InterfazCliente()
	{
		setSize(new Dimension(500, 500));
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		
		botonCrear = new JButton(CREAR);
		botonCrear.addActionListener(this);
		botonCrear.setActionCommand(CREAR);
		add(botonCrear);
		
	}
	
	@Override
	public void actionPerformed(ActionEvent evento) {
		String e = evento.getActionCommand();
		if(e.equals(CREAR))
		{
			Cliente cliente = new Cliente();
			cliente.peticion();
		}
	}
	
	public static void main(String[] args)
	{
		InterfazCliente interfaz = new InterfazCliente();
		interfaz.setVisible(true);
	}
}
