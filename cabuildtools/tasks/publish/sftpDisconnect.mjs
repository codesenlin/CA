export default function (context, args) {
	var connection = args.session.connection;
	connection.end();
}